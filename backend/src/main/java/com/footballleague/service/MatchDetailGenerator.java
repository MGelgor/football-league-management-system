package com.footballleague.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.ToIntFunction;

import org.springframework.stereotype.Component;

import com.footballleague.entity.Match;
import com.footballleague.entity.MatchAppearance;
import com.footballleague.entity.MatchEvent;
import com.footballleague.entity.MatchEventType;
import com.footballleague.entity.MatchTeamStats;
import com.footballleague.entity.Player;
import com.footballleague.entity.Position;
import com.footballleague.entity.Team;

/**
 * Skoru belli olan bir maçı dakika dakika canlandırır: ilk 11, kartlar, sakatlıklar, oyuncu değişiklikleri,
 * goller / asistler, takım istatistikleri ve oyuncu reytingleri.
 * Kurallar:
 * - İlk 11 dizilişe (1 KL, 4 DEF, 4 OS, 2 FV) göre, güçlü oyuncular öncelikli seçilir; en fazla 3 değişiklik
 * - Gol / asist / kart yalnızca o dakikada sahada olan oyunculara yazılır
 * - Goller ve asistler mevki ağırlığı × oyuncu gücü ile dağıtılır (kaleci gol atmaz)
 * - Kartlar iki takıma aynı ortalamayla verilir; ikinci sarı kırmızıya döner, kırmızı gören oyuncu oyundan çıkar
 *   ve yerine kimse giremez; sakatlanan oyuncunun yerine (değişiklik hakkı varsa) yedek girer
 * - şut >= isabetli şut >= gol, kurtarış = rakibin isabetli şutu - rakibin golü, faul >= kart,
 *   topla oynama oranları toplamı 100
 */
@Component
public class MatchDetailGenerator {

    static final int MATCH_MINUTES = 90;
    static final int MAX_SUBSTITUTIONS = 3;
    static final int STARTING_PLAYERS = 11;
    static final Map<Position, Integer> FORMATION = new EnumMap<>(Map.of(
            Position.GOALKEEPER, 1,
            Position.DEFENDER, 4,
            Position.MIDFIELDER, 4,
            Position.FORWARD, 2));

    private static final double YELLOW_CARDS_PER_TEAM = 1.8;
    private static final double DIRECT_RED_CHANCE = 0.04;
    private static final double ASSIST_CHANCE = 0.7;
    // İlk 11'deki her oyuncu için maç içinde sakatlanma olasılığı
    private static final double INJURY_CHANCE = 0.008;
    // İlk 11 seçiminde güce eklenen rastgelelik (aynı güçteki oyuncular dönüşümlü oynasın)
    private static final int LINEUP_RANDOMNESS = 15;

    public GeneratedDetail generate(Match match, List<Player> homeAvailable, List<Player> awayAvailable,
            ScoreSimulator.ExpectedGoals expected) {
        SideResult home = simulateSide(match, match.getHomeTeam(), homeAvailable, match.getHomeScore());
        SideResult away = simulateSide(match, match.getAwayTeam(), awayAvailable, match.getAwayScore());

        List<MatchEvent> events = new ArrayList<>(home.events());
        events.addAll(away.events());
        events.sort(Comparator.comparingInt(MatchEvent::getMinute));

        ThreadLocalRandom random = ThreadLocalRandom.current();
        double homeShare = expected.home() / (expected.home() + expected.away());
        int homePossession = Math.clamp(Math.round(50 + (homeShare - 0.5) * 40) + random.nextInt(-5, 6), 30, 70);

        MatchTeamStats homeStats = sideStats(match, match.getHomeTeam(), true, homePossession,
                match.getHomeScore(), expected.home(), cardCount(home.events()));
        MatchTeamStats awayStats = sideStats(match, match.getAwayTeam(), false, 100 - homePossession,
                match.getAwayScore(), expected.away(), cardCount(away.events()));
        homeStats.setSaves(awayStats.getShotsOnTarget() - match.getAwayScore());
        awayStats.setSaves(homeStats.getShotsOnTarget() - match.getHomeScore());

        rate(home, match.getHomeScore(), match.getAwayScore(), homeStats.getSaves());
        rate(away, match.getAwayScore(), match.getHomeScore(), awayStats.getSaves());

        List<MatchAppearance> appearances = new ArrayList<>(home.appearances());
        appearances.addAll(away.appearances());
        appearances.stream()
                .max(Comparator.comparingDouble(MatchAppearance::getRating))
                .ifPresent(best -> best.setPlayerOfTheMatch(true));

        return new GeneratedDetail(events, appearances, homeStats, awayStats);
    }

    private SideResult simulateSide(Match match, Team team, List<Player> available, int goals) {
        if (available.isEmpty()) {
            return new SideResult(List.of(), List.of());
        }
        return new SideSimulation(match, team, available).run(goals);
    }

    /** Bir takımın maç içi durumu: sahadakiler, yedekler, kalan değişiklik hakkı. */
    private static final class SideSimulation {

        private enum Kind { CARD, INJURY, SUBSTITUTION, GOAL }

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

        private SideSimulation(Match match, Team team, List<Player> available) {
            this.match = match;
            this.team = team;
            List<Player> starters = pickStartingEleven(available);
            onPitch.addAll(starters);
            available.stream().filter(player -> !starters.contains(player)).forEach(bench::add);
            for (Player starter : starters) {
                appearances.put(starter, MatchAppearance.builder()
                        .match(match).team(team).player(starter)
                        .starter(true).minuteOn(0).minuteOff(MATCH_MINUTES)
                        .rating(0.0)
                        .build());
            }
        }

        private SideResult run(int goals) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            List<Incident> incidents = new ArrayList<>();
            int yellowCount = ScoreSimulator.poissonRandom(YELLOW_CARDS_PER_TEAM);
            for (int i = 0; i < yellowCount; i++) {
                incidents.add(new Incident(randomMinute(), Kind.CARD, false));
            }
            if (random.nextDouble() < DIRECT_RED_CHANCE) {
                incidents.add(new Incident(randomMinute(), Kind.CARD, true));
            }
            for (int i = 0; i < onPitch.size(); i++) {
                if (random.nextDouble() < INJURY_CHANCE) {
                    incidents.add(new Incident(random.nextInt(1, MATCH_MINUTES), Kind.INJURY, false));
                }
            }
            random.ints(46, MATCH_MINUTES - 1).distinct().limit(random.nextInt(1, MAX_SUBSTITUTIONS + 1))
                    .forEach(minute -> incidents.add(new Incident(minute, Kind.SUBSTITUTION, false)));
            for (int i = 0; i < goals; i++) {
                incidents.add(new Incident(randomMinute(), Kind.GOAL, false));
            }
            // Aynı dakikada önce kartlar, sonra sakatlık ve değişiklik, en son gol işlenir
            incidents.sort(Comparator.comparingInt(Incident::minute).thenComparing(Incident::kind));

            for (Incident incident : incidents) {
                switch (incident.kind()) {
                    case CARD -> card(incident.minute(), incident.directRed());
                    case INJURY -> injury(incident.minute());
                    case SUBSTITUTION -> tacticalSubstitution(incident.minute());
                    case GOAL -> goal(incident.minute());
                }
            }
            return new SideResult(events, new ArrayList<>(appearances.values()));
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
            bringOn(player, minute);
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
            bringOn(outgoing, minute);
        }

        private void goal(int minute) {
            Player scorer = pickWeighted(onPitch, player -> goalWeight(player.getPosition()) * player.getStrength());
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
                        player -> assistWeight(player.getPosition()) * player.getStrength());
            }
            events.add(event(scorer, assist, MatchEventType.GOAL, minute));
        }

        private void takeOff(Player player, int minute) {
            onPitch.remove(player);
            appearances.get(player).setMinuteOff(minute);
        }

        /** Değişiklik hakkı ve yedek varsa çıkan oyuncunun yerine (tercihen aynı mevkiden) yedek girer. */
        private void bringOn(Player outgoing, int minute) {
            if (substitutionsLeft == 0 || bench.isEmpty()) {
                return;
            }
            Player incoming = bench.stream()
                    .filter(player -> player.getPosition() == outgoing.getPosition())
                    .max(Comparator.comparingInt(Player::getStrength))
                    .orElseGet(() -> bench.stream().max(Comparator.comparingInt(Player::getStrength)).orElseThrow());
            bench.remove(incoming);
            onPitch.add(incoming);
            substitutionsLeft--;
            appearances.put(incoming, MatchAppearance.builder()
                    .match(match).team(team).player(incoming)
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

    /** Dizilişteki her mevki için (güç + rastgelelik) en yüksek oyuncular; eksik mevki kalan en iyilerle doldurulur. */
    static List<Player> pickStartingEleven(List<Player> available) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Map<Player, Integer> score = new IdentityHashMap<>();
        available.forEach(player -> score.put(player, player.getStrength() + random.nextInt(LINEUP_RANDOMNESS + 1)));
        Comparator<Player> best = Comparator.comparingInt((Player player) -> score.get(player)).reversed();

        List<Player> starters = new ArrayList<>();
        for (Map.Entry<Position, Integer> slot : FORMATION.entrySet()) {
            available.stream()
                    .filter(player -> player.getPosition() == slot.getKey())
                    .sorted(best)
                    .limit(slot.getValue())
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
    private static void rate(SideResult side, int goalsFor, int goalsAgainst, int saves) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double resultBonus = Integer.compare(goalsFor, goalsAgainst) * 0.4;

        for (MatchAppearance appearance : side.appearances()) {
            Player player = appearance.getPlayer();
            double share = appearance.minutesPlayed() / (double) MATCH_MINUTES;
            double rating = random.nextDouble(-0.4, 0.4) + resultBonus * share;

            for (MatchEvent event : side.events()) {
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
            }
            if (player.getPosition() == Position.GOALKEEPER) {
                rating += saves * 0.15 * share - goalsAgainst * 0.25 * share;
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

    private MatchTeamStats sideStats(Match match, Team team, boolean home, int possession, int goals,
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

    private static int randomMinute() {
        return ThreadLocalRandom.current().nextInt(1, MATCH_MINUTES + 1);
    }

    private record SideResult(List<MatchEvent> events, List<MatchAppearance> appearances) {
    }

    public record GeneratedDetail(List<MatchEvent> events, List<MatchAppearance> appearances,
            MatchTeamStats homeStats, MatchTeamStats awayStats) {
    }
}
