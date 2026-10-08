package com.footballleague.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.ToIntFunction;

import org.springframework.stereotype.Component;

import com.footballleague.entity.Formation;
import com.footballleague.entity.Match;
import com.footballleague.entity.MatchAppearance;
import com.footballleague.entity.MatchEvent;
import com.footballleague.entity.MatchEventType;
import com.footballleague.entity.MatchTeamStats;
import com.footballleague.entity.Player;
import com.footballleague.entity.Position;
import com.footballleague.entity.Referee;
import com.footballleague.entity.Team;

/**
 * Bir maçı dakika dakika canlandırır: ilk 11, kartlar, sakatlıklar, oyuncu değişiklikleri, goller / asistler,
 * takım istatistikleri ve oyuncu reytingleri. Maç parça parça (ör. iki devre) oynatılabilir: {@link LiveMatch}
 * devre arasında durur, kullanıcı değişiklik yapabilir, sonra ikinci yarı oynanır.
 * Kurallar:
 * - İlk 11 takımın dizilişine (ör. 4-4-2: 1 KL, 4 DEF, 4 OS, 2 FV) göre, güçlü oyuncular öncelikli seçilir
 *   (ya da kullanıcı seçer); en fazla 3 değişiklik
 * - Gol / asist / kart yalnızca o dakikada sahada olan oyunculara yazılır
 * - Goller ve asistler mevki ağırlığı × efektif güç ile dağıtılır (kaleci gol atmaz)
 * - Kartlar iki takıma aynı ortalamayla verilir (hakem sertliğiyle çarpılır); ikinci sarı kırmızıya döner, kırmızı
 *   gören oyuncu oyundan çıkar ve yerine kimse giremez; sakatlanan oyuncunun yerine (değişiklik hakkı varsa) yedek girer
 * - Gollerin bir kısmı penaltıdan (takımın penaltıcısı atar), çok azı rakibin kendi kalesine golü; ayrıca kaçan
 *   penaltılar (~%22) ve VAR'ın iptal ettiği goller skoru değiştirmeden olay olarak yazılır
 * - şut >= isabetli şut >= gol, kurtarış = rakibin isabetli şutu - rakibin golü, faul >= kart,
 *   topla oynama oranları toplamı 100
 */
@Component
public class MatchDetailGenerator {

    static final int MATCH_MINUTES = 90;
    static final int HALF_TIME = 45;
    static final int MAX_SUBSTITUTIONS = 3;
    static final int STARTING_PLAYERS = 11;

    private static final double YELLOW_CARDS_PER_TEAM = 1.8;
    private static final double DIRECT_RED_CHANCE = 0.04;
    private static final double ASSIST_CHANCE = 0.7;
    // Ortalama hakemde bir golün penaltıdan gelme olasılığı
    static final double PENALTY_GOAL_SHARE = 0.09;
    static final double PENALTY_SUCCESS = 0.78;
    static final double OWN_GOAL_SHARE = 0.04;
    private static final double VAR_DISALLOWED_CHANCE = 0.1;
    // İlk 11'deki her oyuncu için maç içinde sakatlanma olasılığı
    private static final double INJURY_CHANCE = 0.008;
    // İlk 11 seçiminde güce eklenen rastgelelik (aynı güçteki oyuncular dönüşümlü oynasın)
    private static final int LINEUP_RANDOMNESS = 15;
    // Yapay zekâ değişiklikleri bu dakikalar arasında yapar
    private static final int FIRST_SUBSTITUTION_MINUTE = 46;
    private static final int LAST_SUBSTITUTION_MINUTE = 88;

    /**
     * Bir takımın maç kurulumu. starters null ise ilk 11 yapay zekâ tarafından seçilir; penaltyTaker null ise
     * sahadaki en uygun oyuncu atar; autoSubstitutions false ise taktik değişiklik yapılmaz (kullanıcı yapar).
     */
    public record SideSetup(List<Player> available, Formation formation, List<Player> starters, Player penaltyTaker,
            boolean autoSubstitutions) {

        public static SideSetup auto(List<Player> available, Formation formation) {
            return new SideSetup(available, formation, null, null, true);
        }
    }

    public GeneratedDetail generate(Match match, List<Player> homeAvailable, List<Player> awayAvailable,
            ScoreSimulator.ExpectedGoals expected) {
        return generate(match, homeAvailable, awayAvailable, expected, Formation.F442, Formation.F442);
    }

    public GeneratedDetail generate(Match match, List<Player> homeAvailable, List<Player> awayAvailable,
            ScoreSimulator.ExpectedGoals expected, Formation homeFormation, Formation awayFormation) {
        return generate(match, SideSetup.auto(homeAvailable, homeFormation), SideSetup.auto(awayAvailable, awayFormation),
                expected);
    }

    /** Skoru belli olan maçı (match.homeScore / awayScore) tek parçada 1-90 oynatır. */
    public GeneratedDetail generate(Match match, SideSetup home, SideSetup away, ScoreSimulator.ExpectedGoals expected) {
        LiveMatch live = start(match, home, away);
        live.play(1, MATCH_MINUTES, match.getHomeScore(), match.getAwayScore(), expected);
        return live.finish(expected);
    }

    public LiveMatch start(Match match, SideSetup home, SideSetup away) {
        return new LiveMatch(match, new SideSimulation(match, match.getHomeTeam(), home),
                new SideSimulation(match, match.getAwayTeam(), away));
    }

    /** Parça parça oynatılan maç: play() ile dakika aralıkları, sonda finish() ile istatistik ve reytingler. */
    public static final class LiveMatch {

        private final Match match;
        private final SideSimulation home;
        private final SideSimulation away;

        private LiveMatch(Match match, SideSimulation home, SideSimulation away) {
            this.match = match;
            this.home = home;
            this.away = away;
        }

        /**
         * from-to dakikalarını oynatır; homeGoals / awayGoals bu aralıkta atılacak goller. expected: tam maç gol
         * beklentisi (kaçan penaltı sayısı aralığın payıyla ölçeklenir).
         */
        public void play(int from, int to, int homeGoals, int awayGoals, ScoreSimulator.ExpectedGoals expected) {
            double share = (to - from + 1) / (double) MATCH_MINUTES;
            int homeStart = home.events.size();
            int awayStart = away.events.size();
            home.playSegment(from, to, homeGoals, expected.home(), share);
            away.playSegment(from, to, awayGoals, expected.away(), share);
            convertOwnGoals(home, away, homeStart);
            convertOwnGoals(away, home, awayStart);
        }

        /** Kullanıcının değişikliği (devre arası): çıkan sahada, giren yedekte olmalı, hak kalmış olmalı. */
        public void substitute(boolean homeSide, Player outgoing, Player incoming, int minute) {
            SideSimulation side = homeSide ? home : away;
            if (side.substitutionsLeft == 0) {
                throw new IllegalArgumentException("Değişiklik hakkı kalmadı");
            }
            if (!side.onPitch.contains(outgoing) || !side.bench.contains(incoming)) {
                throw new IllegalArgumentException("Değişiklik geçersiz: çıkan sahada, giren yedekte olmalı");
            }
            side.takeOff(outgoing, minute);
            side.bringOn(outgoing, incoming, minute);
        }

        public SideView side(boolean homeSide) {
            SideSimulation side = homeSide ? home : away;
            return new SideView(List.copyOf(side.onPitch), List.copyOf(side.bench), side.substitutionsLeft);
        }

        public int homeGoals() {
            return goals(home, away);
        }

        public int awayGoals() {
            return goals(away, home);
        }

        /** Şimdiye kadarki olaylar (dakika sırasıyla) ve oyuna girenler. */
        public List<MatchEvent> events() {
            List<MatchEvent> events = new ArrayList<>(home.events);
            events.addAll(away.events);
            events.sort(Comparator.comparingInt(MatchEvent::getMinute));
            return events;
        }

        public List<MatchAppearance> appearances() {
            List<MatchAppearance> appearances = new ArrayList<>(home.appearances.values());
            appearances.addAll(away.appearances.values());
            return appearances;
        }

        public Match match() {
            return match;
        }

        /** Maç sonu: takım istatistikleri, reytingler, maçın oyuncusu. expected: tam maç gol beklentisi. */
        public GeneratedDetail finish(ScoreSimulator.ExpectedGoals expected) {
            int homeGoals = homeGoals();
            int awayGoals = awayGoals();
            ThreadLocalRandom random = ThreadLocalRandom.current();
            double homeShare = expected.home() / (expected.home() + expected.away());
            int homePossession = Math.clamp(Math.round(50 + (homeShare - 0.5) * 40) + random.nextInt(-5, 6), 30, 70);

            MatchTeamStats homeStats = sideStats(match, home.team, true, homePossession, homeGoals, expected.home(),
                    cardCount(home.events));
            MatchTeamStats awayStats = sideStats(match, away.team, false, 100 - homePossession, awayGoals,
                    expected.away(), cardCount(away.events));
            homeStats.setSaves(awayStats.getShotsOnTarget() - awayGoals);
            awayStats.setSaves(homeStats.getShotsOnTarget() - homeGoals);

            rate(home, homeGoals, awayGoals, homeStats.getSaves(), missedPenalties(away));
            rate(away, awayGoals, homeGoals, awayStats.getSaves(), missedPenalties(home));

            List<MatchAppearance> appearances = appearances();
            appearances.stream()
                    .max(Comparator.comparingDouble(MatchAppearance::getRating))
                    .ifPresent(best -> best.setPlayerOfTheMatch(true));
            return new GeneratedDetail(events(), appearances, homeStats, awayStats);
        }

        private static int goals(SideSimulation side, SideSimulation opponent) {
            return (int) (side.events.stream().filter(event -> event.getType() == MatchEventType.GOAL).count()
                    + opponent.events.stream().filter(event -> event.getType() == MatchEventType.OWN_GOAL).count());
        }
    }

    /** Devre arasında bir takımın durumu: sahadakiler, yedekler, kalan değişiklik hakkı. */
    public record SideView(List<Player> onPitch, List<Player> bench, int substitutionsLeft) {
    }

    /**
     * scorer tarafının (fromIndex'ten sonraki) penaltı olmayan gollerinin bir kısmı, o dakikada sahada olan bir
     * rakip oyuncunun kendi kalesine golüne çevrilir: skor değişmez, olay rakip tarafa (oyuncunun takımına) geçer.
     */
    private static void convertOwnGoals(SideSimulation scorer, SideSimulation opponent, int fromIndex) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (MatchEvent goal : List.copyOf(scorer.events.subList(fromIndex, scorer.events.size()))) {
            if (goal.getType() != MatchEventType.GOAL || goal.isPenalty() || random.nextDouble() >= OWN_GOAL_SHARE) {
                continue;
            }
            List<Player> onPitch = opponent.appearances.values().stream()
                    .filter(appearance -> appearance.getMinuteOn() <= goal.getMinute()
                            && (goal.getMinute() < appearance.getMinuteOff() || appearance.getMinuteOff() == MATCH_MINUTES))
                    .map(MatchAppearance::getPlayer)
                    .toList();
            Player unlucky = pickWeighted(onPitch, player -> ownGoalWeight(player.getPosition()));
            if (unlucky == null) {
                continue;
            }
            // Kaydedilmemiş olayların id'si yok, equals hepsini eşit sayar; kimlikle silinir
            scorer.events.removeIf(event -> event == goal);
            opponent.events.add(MatchEvent.builder()
                    .match(goal.getMatch())
                    .team(opponent.team)
                    .player(unlucky)
                    .type(MatchEventType.OWN_GOAL)
                    .minute(goal.getMinute())
                    .build());
        }
    }

    private static int missedPenalties(SideSimulation side) {
        return (int) side.events.stream().filter(event -> event.getType() == MatchEventType.PENALTY_MISSED).count();
    }

    /** Bir takımın maç içi durumu: sahadakiler, yedekler, kalan değişiklik hakkı. */
    private static final class SideSimulation {

        private enum Kind { CARD, INJURY, SUBSTITUTION, VAR, PENALTY_MISS, GOAL }

        private record Incident(int minute, Kind kind, boolean directRed) {
        }

        private final Match match;
        private final Team team;
        private final List<Player> onPitch = new ArrayList<>();
        private final List<Player> bench = new ArrayList<>();
        // Oyuncu → maç kaydı; LinkedHashMap ile ilk 11 önce, girenler giriş sırasıyla
        private final Map<Player, MatchAppearance> appearances = new LinkedHashMap<>();
        private final Map<Player, Integer> yellows = new IdentityHashMap<>();
        private final List<MatchEvent> events = new ArrayList<>();
        private int substitutionsLeft = MAX_SUBSTITUTIONS;
        private final double cardFactor;
        private final double penaltyFactor;
        private final Player chosenPenaltyTaker;
        private final boolean autoSubstitutions;

        private SideSimulation(Match match, Team team, SideSetup setup) {
            this.match = match;
            this.team = team;
            this.cardFactor = Referee.cardFactor(match.getReferee());
            this.penaltyFactor = Referee.penaltyFactor(match.getReferee());
            this.chosenPenaltyTaker = setup.penaltyTaker();
            this.autoSubstitutions = setup.autoSubstitutions();
            List<Player> starters = setup.starters() != null
                    ? setup.starters()
                    : pickStartingEleven(setup.available(), setup.formation());
            onPitch.addAll(starters);
            setup.available().stream().filter(player -> !starters.contains(player)).forEach(bench::add);
            for (Player starter : starters) {
                appearances.put(starter, MatchAppearance.builder()
                        .match(match).team(team).player(starter)
                        .starter(true).minuteOn(0).minuteOff(MATCH_MINUTES)
                        .rating(0.0)
                        .build());
            }
        }

        /** share: aralığın maça oranı (kart, sakatlık, kaçan penaltı ve VAR olasılıkları bununla ölçeklenir). */
        private void playSegment(int from, int to, int goals, double expectedGoals, double share) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            List<Incident> incidents = new ArrayList<>();
            int yellowCount = ScoreSimulator.poissonRandom(YELLOW_CARDS_PER_TEAM * cardFactor * share);
            for (int i = 0; i < yellowCount; i++) {
                incidents.add(new Incident(randomMinute(from, to), Kind.CARD, false));
            }
            // Atılan penaltı golü / kaçan penaltı oranı ~ PENALTY_SUCCESS / (1 - PENALTY_SUCCESS)
            int missedPenalties = ScoreSimulator.poissonRandom(expectedGoals * PENALTY_GOAL_SHARE * penaltyFactor
                    * (1 - PENALTY_SUCCESS) / PENALTY_SUCCESS * share);
            for (int i = 0; i < missedPenalties; i++) {
                incidents.add(new Incident(randomMinute(from, to), Kind.PENALTY_MISS, false));
            }
            if (random.nextDouble() < VAR_DISALLOWED_CHANCE * share) {
                incidents.add(new Incident(randomMinute(from, to), Kind.VAR, false));
            }
            if (random.nextDouble() < DIRECT_RED_CHANCE * cardFactor * share) {
                incidents.add(new Incident(randomMinute(from, to), Kind.CARD, true));
            }
            int lastInjuryMinute = Math.min(to, MATCH_MINUTES - 1);
            for (int i = 0; i < onPitch.size() && from <= lastInjuryMinute; i++) {
                if (random.nextDouble() < INJURY_CHANCE * share) {
                    incidents.add(new Incident(random.nextInt(from, lastInjuryMinute + 1), Kind.INJURY, false));
                }
            }
            int firstSub = Math.max(FIRST_SUBSTITUTION_MINUTE, from);
            int lastSub = Math.min(LAST_SUBSTITUTION_MINUTE, to);
            if (autoSubstitutions && firstSub <= lastSub && substitutionsLeft > 0) {
                random.ints(firstSub, lastSub + 1).distinct()
                        .limit(Math.min(random.nextInt(1, MAX_SUBSTITUTIONS + 1), lastSub - firstSub + 1))
                        .forEach(minute -> incidents.add(new Incident(minute, Kind.SUBSTITUTION, false)));
            }
            for (int i = 0; i < goals; i++) {
                incidents.add(new Incident(randomMinute(from, to), Kind.GOAL, false));
            }
            // Aynı dakikada önce kartlar, sonra sakatlık ve değişiklik, en son gol işlenir
            incidents.sort(Comparator.comparingInt(Incident::minute).thenComparing(Incident::kind));

            for (Incident incident : incidents) {
                switch (incident.kind()) {
                    case CARD -> card(incident.minute(), incident.directRed());
                    case INJURY -> injury(incident.minute());
                    case SUBSTITUTION -> tacticalSubstitution(incident.minute());
                    case VAR -> disallowedGoal(incident.minute());
                    case PENALTY_MISS -> missedPenalty(incident.minute());
                    case GOAL -> goal(incident.minute());
                }
            }
        }

        private void disallowedGoal(int minute) {
            Player player = pickWeighted(onPitch, candidate -> goalWeight(candidate.getPosition()));
            if (player != null) {
                events.add(event(player, null, MatchEventType.VAR_DISALLOWED, minute));
            }
        }

        private void missedPenalty(int minute) {
            Player taker = penaltyTaker();
            if (taker != null) {
                MatchEvent miss = event(taker, null, MatchEventType.PENALTY_MISSED, minute);
                miss.setPenalty(true);
                events.add(miss);
            }
        }

        /** Seçilen penaltıcı sahadaysa o; değilse sahadaki en güçlü forvet / orta saha (forvet önce). */
        private Player penaltyTaker() {
            if (chosenPenaltyTaker != null && onPitch.contains(chosenPenaltyTaker)) {
                return chosenPenaltyTaker;
            }
            return onPitch.stream()
                    .filter(player -> player.getPosition() != Position.GOALKEEPER)
                    .max(Comparator.comparingInt(player -> player.getStrength() + switch (player.getPosition()) {
                        case FORWARD -> 10;
                        case MIDFIELDER -> 5;
                        default -> 0;
                    }))
                    .orElse(null);
        }

        private void card(int minute, boolean directRed) {
            Player player = pickWeighted(onPitch, candidate -> cardWeight(candidate.getPosition()));
            if (player == null) {
                return;
            }
            if (!directRed) {
                events.add(event(player, null, MatchEventType.YELLOW_CARD, minute));
                if (yellows.merge(player, 1, Integer::sum) < 2) {
                    return;
                }
            }
            events.add(event(player, null, MatchEventType.RED_CARD, minute));
            takeOff(player, minute);
        }

        private void injury(int minute) {
            if (onPitch.isEmpty()) {
                return;
            }
            Player player = onPitch.get(ThreadLocalRandom.current().nextInt(onPitch.size()));
            events.add(event(player, null, MatchEventType.INJURY, minute));
            takeOff(player, minute);
            bringOn(player, null, minute);
        }

        private void tacticalSubstitution(int minute) {
            if (substitutionsLeft == 0 || bench.isEmpty()) {
                return;
            }
            // Zayıf oyuncunun oyundan alınma olasılığı daha yüksek; kaleci değiştirilmez
            Player outgoing = pickWeighted(
                    onPitch.stream().filter(player -> player.getPosition() != Position.GOALKEEPER).toList(),
                    player -> Player.MAX_STRENGTH + 1 - player.getStrength());
            if (outgoing == null) {
                return;
            }
            takeOff(outgoing, minute);
            bringOn(outgoing, null, minute);
        }

        private void goal(int minute) {
            Player taker = penaltyTaker();
            if (taker != null && ThreadLocalRandom.current().nextDouble() < PENALTY_GOAL_SHARE * penaltyFactor) {
                MatchEvent penaltyGoal = event(taker, null, MatchEventType.GOAL, minute);
                penaltyGoal.setPenalty(true);
                events.add(penaltyGoal);
                return;
            }
            Player scorer = pickWeighted(onPitch, player -> weighted(goalWeight(player.getPosition()), player));
            if (scorer == null && !onPitch.isEmpty()) {
                // Sahada yalnızca kaleci kaldıysa skor tutarlı kalsın diye gol ona yazılır
                scorer = onPitch.getFirst();
            }
            if (scorer == null) {
                return;
            }
            Player assist = null;
            if (ThreadLocalRandom.current().nextDouble() < ASSIST_CHANCE) {
                Player finalScorer = scorer;
                assist = pickWeighted(onPitch.stream().filter(player -> player != finalScorer).toList(),
                        player -> weighted(assistWeight(player.getPosition()), player));
            }
            events.add(event(scorer, assist, MatchEventType.GOAL, minute));
        }

        private void takeOff(Player player, int minute) {
            onPitch.remove(player);
            appearances.get(player).setMinuteOff(minute);
        }

        /**
         * Değişiklik hakkı ve yedek varsa çıkanın yerine yedek girer: incoming verilmişse o, verilmemişse yedeklerden
         * (tercihen aynı mevkiden) en güçlüsü.
         */
        private void bringOn(Player outgoing, Player incoming, int minute) {
            if (substitutionsLeft == 0 || bench.isEmpty()) {
                return;
            }
            Player chosen = incoming != null ? incoming : bench.stream()
                    .filter(player -> player.getPosition() == outgoing.getPosition())
                    .max(Comparator.comparingInt(Player::getStrength))
                    .orElseGet(() -> bench.stream().max(Comparator.comparingInt(Player::getStrength)).orElseThrow());
            bench.remove(chosen);
            onPitch.add(chosen);
            substitutionsLeft--;
            appearances.put(chosen, MatchAppearance.builder()
                    .match(match).team(team).player(chosen)
                    .starter(false).minuteOn(minute).minuteOff(MATCH_MINUTES)
                    .replacedPlayer(outgoing)
                    .rating(0.0)
                    .build());
        }

        private MatchEvent event(Player player, Player assist, MatchEventType type, int minute) {
            return MatchEvent.builder()
                    .match(match)
                    .team(team)
                    .player(player)
                    .assistPlayer(assist)
                    .type(type)
                    .minute(minute)
                    .build();
        }
    }

    /** Mevki ağırlığı × efektif güç (form ve yorgunluk dahil). */
    private static int weighted(int positionWeight, Player player) {
        return (int) Math.round(positionWeight * player.effectiveStrength());
    }

    /**
     * Dizilişteki her mevki için (efektif güç + rastgelelik) en yüksek oyuncular; eksik mevki kalan en iyilerle
     * doldurulur. Efektif güç formu iyi olanı öne, üst üste çok oynayanı (yorgun) geriye iter.
     */
    static List<Player> pickStartingEleven(List<Player> available, Formation formation) {
        return pickStartingEleven(available, formation, LINEUP_RANDOMNESS);
    }

    /** randomness 0: rastgelelik olmadan en iyi 11 (kullanıcıya önerilen kadro). */
    public static List<Player> pickStartingEleven(List<Player> available, Formation formation, int randomness) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Map<Player, Double> score = new IdentityHashMap<>();
        available.forEach(player -> score.put(player,
                player.effectiveStrength() + (randomness > 0 ? random.nextInt(randomness + 1) : 0)));
        Comparator<Player> best = Comparator.comparingDouble((Player player) -> score.get(player)).reversed();

        List<Player> starters = new ArrayList<>();
        for (Position position : Position.values()) {
            available.stream()
                    .filter(player -> player.getPosition() == position)
                    .sorted(best)
                    .limit(formation.count(position))
                    .forEach(starters::add);
        }
        available.stream()
                .filter(player -> !starters.contains(player))
                .sorted(Comparator.comparing((Player player) -> player.getPosition() == Position.GOALKEEPER)
                        .thenComparing(best))
                .limit(Math.max(0, STARTING_PLAYERS - starters.size()))
                .forEach(starters::add);
        return starters;
    }

    /**
     * 6.0 taban + gol (1.0) + asist (0.6) + sonuç (±0.4) + gol yememe (KL 0.8, DEF 0.5) + kaleci kurtarışları
     * − yenilen gol − kartlar; 20 dakikadan az oynayanlarda etki yarıya iner. 3.0 - 10.0 arası, tek ondalık.
     */
    private static void rate(SideSimulation side, int goalsFor, int goalsAgainst, int saves, int opponentMissedPenalties) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double resultBonus = Integer.compare(goalsFor, goalsAgainst) * 0.4;

        for (MatchAppearance appearance : side.appearances.values()) {
            Player player = appearance.getPlayer();
            double share = appearance.minutesPlayed() / (double) MATCH_MINUTES;
            double rating = random.nextDouble(-0.4, 0.4) + resultBonus * share;

            for (MatchEvent event : side.events) {
                if (event.getType() == MatchEventType.GOAL && event.getPlayer() == player) {
                    rating += 1.0;
                }
                if (event.getType() == MatchEventType.GOAL && event.getAssistPlayer() == player) {
                    rating += 0.6;
                }
                if (event.getType() == MatchEventType.YELLOW_CARD && event.getPlayer() == player) {
                    rating -= 0.4;
                }
                if (event.getType() == MatchEventType.RED_CARD && event.getPlayer() == player) {
                    rating -= 1.5;
                }
                if (event.getType() == MatchEventType.OWN_GOAL && event.getPlayer() == player) {
                    rating -= 1.0;
                }
                if (event.getType() == MatchEventType.PENALTY_MISSED && event.getPlayer() == player) {
                    rating -= 0.8;
                }
            }
            if (player.getPosition() == Position.GOALKEEPER) {
                rating += saves * 0.15 * share - goalsAgainst * 0.25 * share;
                // Kaçan penaltıların hepsi kalecinin kurtarışı sayılır
                rating += opponentMissedPenalties * 0.6 * share;
                rating += goalsAgainst == 0 && share >= 2.0 / 3 ? 0.8 : 0;
            } else if (player.getPosition() == Position.DEFENDER) {
                rating -= goalsAgainst * 0.1 * share;
                rating += goalsAgainst == 0 && share >= 2.0 / 3 ? 0.5 : 0;
            }
            if (appearance.minutesPlayed() < 20) {
                rating *= 0.5;
            }
            appearance.setRating(Math.round(Math.clamp(6.0 + rating, 3.0, 10.0) * 10) / 10.0);
        }
    }

    private static MatchTeamStats sideStats(Match match, Team team, boolean home, int possession, int goals,
            double expectedGoals, int cards) {
        int shotsOnTarget = goals + ScoreSimulator.poissonRandom(1.5 + expectedGoals * 1.5);
        int shots = shotsOnTarget + ScoreSimulator.poissonRandom(4 + expectedGoals * 3);
        return MatchTeamStats.builder()
                .match(match)
                .team(team)
                .home(home)
                .possession(possession)
                .shots(shots)
                .shotsOnTarget(shotsOnTarget)
                .corners(ScoreSimulator.poissonRandom(2.5 + expectedGoals * 2))
                .fouls(cards + ScoreSimulator.poissonRandom(9))
                .offsides(ScoreSimulator.poissonRandom(1.8))
                .saves(0)
                .build();
    }

    private static int cardCount(List<MatchEvent> events) {
        return (int) events.stream()
                .filter(event -> event.getType() == MatchEventType.YELLOW_CARD
                        || event.getType() == MatchEventType.RED_CARD)
                .count();
    }

    /** İlk 11'de 2 forvet / 4 orta saha / 4 defans varken golleri ~%50 / %35 / %15 dağıtır. */
    private static int goalWeight(Position position) {
        return switch (position) {
            case FORWARD -> 15;
            case MIDFIELDER -> 5;
            case DEFENDER -> 2;
            case GOALKEEPER -> 0;
        };
    }

    private static int assistWeight(Position position) {
        return switch (position) {
            case MIDFIELDER -> 6;
            case FORWARD -> 4;
            case DEFENDER -> 3;
            case GOALKEEPER -> 0;
        };
    }

    private static int ownGoalWeight(Position position) {
        return switch (position) {
            case DEFENDER -> 5;
            case MIDFIELDER -> 2;
            case GOALKEEPER, FORWARD -> 1;
        };
    }

    private static int cardWeight(Position position) {
        return switch (position) {
            case DEFENDER -> 5;
            case MIDFIELDER -> 4;
            case FORWARD -> 3;
            case GOALKEEPER -> 1;
        };
    }

    private static Player pickWeighted(List<Player> players, ToIntFunction<Player> weight) {
        int total = players.stream().mapToInt(weight).sum();
        if (total == 0) {
            return null;
        }
        int roll = ThreadLocalRandom.current().nextInt(total);
        for (Player player : players) {
            roll -= weight.applyAsInt(player);
            if (roll < 0) {
                return player;
            }
        }
        throw new IllegalStateException("Ağırlıklı seçim başarısız");
    }

    private static int randomMinute(int from, int to) {
        return ThreadLocalRandom.current().nextInt(from, to + 1);
    }

    public record GeneratedDetail(List<MatchEvent> events, List<MatchAppearance> appearances,
            MatchTeamStats homeStats, MatchTeamStats awayStats) {
    }
}
