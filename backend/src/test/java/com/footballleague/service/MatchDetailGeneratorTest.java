package com.footballleague.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.footballleague.entity.Match;
import com.footballleague.entity.MatchAppearance;
import com.footballleague.entity.MatchEvent;
import com.footballleague.entity.MatchEventType;
import com.footballleague.entity.MatchTeamStats;
import com.footballleague.entity.Player;
import com.footballleague.entity.Position;
import com.footballleague.entity.Team;

class MatchDetailGeneratorTest {

    private static final int RUNS = 2000;

    private final MatchDetailGenerator generator = new MatchDetailGenerator();
    private final ScoreSimulator scoreSimulator = new ScoreSimulator();
    private final SquadGenerator squadGenerator = new SquadGenerator();

    private final Team home = Team.builder().id(1L).name("Ev").build();
    private final Team away = Team.builder().id(2L).name("Deplasman").build();
    private final List<Player> homeSquad = withIds(squadGenerator.generate(home), 100);
    private final List<Player> awaySquad = withIds(squadGenerator.generate(away), 200);

    @Test
    void golOlaylariSkorlaBirebirUyusurVeGolcuKendiTakimindan() {
        for (int i = 0; i < RUNS; i++) {
            Match match = randomPlayedMatch();
            List<MatchEvent> events = generate(match).events();

            List<MatchEvent> goals = events.stream().filter(event -> event.getType() == MatchEventType.GOAL).toList();
            assertEquals(match.getHomeScore(), (int) goals.stream().filter(event -> event.getTeam() == home).count());
            assertEquals(match.getAwayScore(), (int) goals.stream().filter(event -> event.getTeam() == away).count());
            for (MatchEvent goal : goals) {
                List<Player> squad = goal.getTeam() == home ? homeSquad : awaySquad;
                assertTrue(squad.contains(goal.getPlayer()), "Golcu kendi takiminin oyuncusu olmali");
                assertTrue(goal.getPlayer().getPosition() != Position.GOALKEEPER, "Kaleci gol atmaz");
                assertTrue(goal.getAssistPlayer() == null || goal.getAssistPlayer() != goal.getPlayer(),
                        "Oyuncu kendi golune asist yapamaz");
                assertTrue(goal.getAssistPlayer() == null || squad.contains(goal.getAssistPlayer()));
            }
            assertTrue(events.stream().allMatch(event -> event.getMinute() >= 1 && event.getMinute() <= 90));
        }
    }

    @Test
    void istatistiklerTutarliKurallaraUyar() {
        for (int i = 0; i < RUNS; i++) {
            Match match = randomPlayedMatch();
            MatchDetailGenerator.GeneratedDetail detail = generate(match);
            MatchTeamStats homeStats = detail.homeStats();
            MatchTeamStats awayStats = detail.awayStats();

            assertEquals(100, homeStats.getPossession() + awayStats.getPossession(), "Topla oynama toplami 100");
            assertSideConsistent(homeStats, match.getHomeScore(), cards(detail.events(), home));
            assertSideConsistent(awayStats, match.getAwayScore(), cards(detail.events(), away));
            assertEquals(awayStats.getShotsOnTarget() - match.getAwayScore(), homeStats.getSaves(),
                    "Kurtaris = rakibin isabetli sutu - rakibin golu");
            assertEquals(homeStats.getShotsOnTarget() - match.getHomeScore(), awayStats.getSaves());
        }
    }

    @Test
    void kirmiziKartGorenOyuncuSonrasindaGolAtamazKartGoremezIkinciSariKirmiziyaDoner() {
        for (int i = 0; i < RUNS; i++) {
            List<MatchEvent> events = generate(randomPlayedMatch()).events();
            Map<Player, Integer> redMinute = new IdentityHashMap<>();
            Map<Player, Integer> yellows = new IdentityHashMap<>();

            for (MatchEvent event : events) {
                Integer sentOff = redMinute.get(event.getPlayer());
                switch (event.getType()) {
                    case GOAL -> {
                        assertTrue(sentOff == null || sentOff > event.getMinute(), "Atilan oyuncu gol atamaz");
                        Integer assistSentOff = event.getAssistPlayer() == null ? null : redMinute.get(event.getAssistPlayer());
                        assertTrue(assistSentOff == null || assistSentOff > event.getMinute(), "Atilan oyuncu asist yapamaz");
                    }
                    case YELLOW_CARD -> {
                        assertTrue(sentOff == null, "Atilan oyuncu tekrar kart goremez");
                        yellows.merge(event.getPlayer(), 1, Integer::sum);
                        assertTrue(yellows.get(event.getPlayer()) <= 2, "Bir oyuncu en fazla 2 sari gorur");
                    }
                    case RED_CARD -> {
                        assertTrue(sentOff == null, "Bir oyuncu tek kirmizi gorur");
                        redMinute.put(event.getPlayer(), event.getMinute());
                    }
                }
            }
            yellows.forEach((player, count) -> {
                if (count == 2) {
                    assertTrue(redMinute.containsKey(player), "Ikinci sari kirmiziya donmeli");
                }
            });
        }
    }

    @Test
    void gollerAgirlikliOlarakForvetlereSonraOrtaSahayaDagilir() {
        Map<Position, Integer> goalsByPosition = new EnumMap<>(Position.class);
        for (int i = 0; i < RUNS; i++) {
            generate(randomPlayedMatch()).events().stream()
                    .filter(event -> event.getType() == MatchEventType.GOAL)
                    .forEach(event -> goalsByPosition.merge(event.getPlayer().getPosition(), 1, Integer::sum));
        }
        int forwards = goalsByPosition.getOrDefault(Position.FORWARD, 0);
        int midfielders = goalsByPosition.getOrDefault(Position.MIDFIELDER, 0);
        int defenders = goalsByPosition.getOrDefault(Position.DEFENDER, 0);

        assertTrue(forwards > midfielders && midfielders > defenders && defenders > 0,
                "FV > OS > DEF olmali: " + goalsByPosition);
        assertEquals(0, goalsByPosition.getOrDefault(Position.GOALKEEPER, 0));
    }

    @Test
    void ayniMevkideGucluOyuncuSahadaGecirdigiDakikaBasinaDahaCokGolAtar() {
        // Kadroda yalnızca iki forvet var ki ikisi de ilk 11'e girsin; diğer herkes aynı güçte
        Team strongTeam = Team.builder().id(3L).name("Guclu").strength(50).build();
        List<Player> squad = withIds(squadGenerator.generate(strongTeam), 300);
        squad.forEach(player -> player.setStrength(50));
        List<Player> forwards = squad.stream().filter(player -> player.getPosition() == Position.FORWARD).toList();
        Player star = forwards.get(0);
        Player weak = forwards.get(1);
        star.setStrength(100);
        weak.setStrength(20);
        forwards.subList(2, forwards.size()).forEach(player -> player.setPosition(Position.MIDFIELDER));
        Match match = Match.builder().homeTeam(strongTeam).awayTeam(away).homeScore(3).awayScore(0).build();

        Map<Player, Integer> goals = new IdentityHashMap<>();
        Map<Player, Integer> minutes = new IdentityHashMap<>();
        for (int i = 0; i < RUNS; i++) {
            MatchDetailGenerator.GeneratedDetail detail =
                    generator.generate(match, squad, awaySquad, new ScoreSimulator.ExpectedGoals(1.3, 1.3));
            detail.events().stream()
                    .filter(event -> event.getType() == MatchEventType.GOAL)
                    .forEach(event -> goals.merge(event.getPlayer(), 1, Integer::sum));
            detail.appearances().forEach(a -> minutes.merge(a.getPlayer(), a.minutesPlayed(), Integer::sum));
        }
        double starRate = (double) goals.getOrDefault(star, 0) / minutes.get(star);
        double weakRate = (double) goals.getOrDefault(weak, 0) / minutes.get(weak);
        // Güç oranı 100/20 = 5: aynı sürede ~5 kat gol
        double ratio = starRate / weakRate;
        assertTrue(ratio > 4 && ratio < 6.5, "Dakika basina gol orani ~5 olmali, oran: " + ratio);
    }

    @Test
    void ilk11EnFazla3DegisiklikVeOlaylarYalnizcaSahadakiOyuncularaYazilir() {
        for (int i = 0; i < RUNS; i++) {
            MatchDetailGenerator.GeneratedDetail detail = generate(randomPlayedMatch());
            for (Team team : List.of(home, away)) {
                List<MatchAppearance> appearances = detail.appearances().stream()
                        .filter(a -> a.getTeam() == team).toList();
                assertEquals(11, appearances.stream().filter(MatchAppearance::isStarter).count(), "Ilk 11");
                long subs = appearances.stream().filter(a -> !a.isStarter()).count();
                assertTrue(subs <= 3, "En fazla 3 degisiklik: " + subs);
                assertEquals(appearances.size(), appearances.stream().map(MatchAppearance::getPlayer).distinct().count(),
                        "Bir oyuncu bir macta tek kayitla oynar");
                assertEquals(1, appearances.stream()
                        .filter(a -> a.isStarter() && a.getPlayer().getPosition() == Position.GOALKEEPER).count(),
                        "Ilk 11'de bir kaleci");
                appearances.stream().filter(a -> !a.isStarter()).forEach(sub -> {
                    assertTrue(sub.getReplacedPlayer() != null, "Oyuna giren birinin yerine girer");
                    MatchAppearance replaced = appearances.stream()
                            .filter(a -> a.getPlayer() == sub.getReplacedPlayer()).findFirst().orElseThrow();
                    assertEquals(sub.getMinuteOn(), replaced.getMinuteOff(), "Cikan oyuncu girenin dakikasinda cikar");
                });
            }
            for (MatchEvent event : detail.events()) {
                MatchAppearance appearance = detail.appearances().stream()
                        .filter(a -> a.getPlayer() == event.getPlayer()).findFirst()
                        .orElseThrow(() -> new AssertionError("Olay sahada olmayan oyuncuya yazildi"));
                assertTrue(appearance.getMinuteOn() <= event.getMinute() && event.getMinute() <= appearance.getMinuteOff(),
                        "Olay oyuncunun sahada oldugu dakikada olmali");
                if (event.getType() == MatchEventType.GOAL) {
                    assertTrue(event.getMinute() < appearance.getMinuteOff() || appearance.getMinuteOff() == 90,
                            "Oyundan cikan oyuncu ayni dakikada gol atamaz");
                }
            }
        }
    }

    @Test
    void kirmiziKartGoreninYerineKimseGirmez() {
        int redCards = 0;
        int injuries = 0;
        for (int i = 0; i < RUNS * 3; i++) {
            MatchDetailGenerator.GeneratedDetail detail = generate(randomPlayedMatch());
            for (MatchEvent event : detail.events()) {
                if (event.getType() == MatchEventType.RED_CARD) {
                    redCards++;
                    assertTrue(detail.appearances().stream().noneMatch(a -> a.getReplacedPlayer() == event.getPlayer()),
                            "Kirmizi kart goren oyuncunun yerine oyuncu girmez");
                }
                if (event.getType() == MatchEventType.INJURY) {
                    injuries++;
                }
            }
        }
        assertTrue(redCards > 0 && injuries > 0, "Ornekte kirmizi kart ve sakatlik olmali");
    }

    @Test
    void reytingler3Ile10ArasindaVeTamOlarakBirMacinOyuncusuVar() {
        for (int i = 0; i < RUNS; i++) {
            MatchDetailGenerator.GeneratedDetail detail = generate(randomPlayedMatch());
            assertTrue(detail.appearances().stream().allMatch(a -> a.getRating() >= 3.0 && a.getRating() <= 10.0));
            assertEquals(1, detail.appearances().stream().filter(MatchAppearance::isPlayerOfTheMatch).count());
            double best = detail.appearances().stream().mapToDouble(MatchAppearance::getRating).max().orElseThrow();
            assertTrue(detail.appearances().stream().filter(MatchAppearance::isPlayerOfTheMatch)
                    .allMatch(a -> a.getRating() == best), "Macin oyuncusu en yuksek reytingli");
        }
    }

    @Test
    void golAtanOyuncununReytingiOrtalamadanYuksek() {
        double scorerSum = 0;
        int scorers = 0;
        double othersSum = 0;
        int others = 0;
        for (int i = 0; i < RUNS; i++) {
            MatchDetailGenerator.GeneratedDetail detail = generate(randomPlayedMatch());
            for (MatchAppearance appearance : detail.appearances()) {
                boolean scored = detail.events().stream().anyMatch(e -> e.getType() == MatchEventType.GOAL
                        && e.getPlayer() == appearance.getPlayer());
                if (scored) {
                    scorerSum += appearance.getRating();
                    scorers++;
                } else {
                    othersSum += appearance.getRating();
                    others++;
                }
            }
        }
        assertTrue(scorerSum / scorers > othersSum / others + 0.7, "Gol atanlar belirgin sekilde yuksek reyting almali");
    }

    @Test
    void kartlarIkiTakimaDengeliDagilir() {
        int homeCards = 0;
        int awayCards = 0;
        for (int i = 0; i < RUNS; i++) {
            List<MatchEvent> events = generate(randomPlayedMatch()).events();
            homeCards += cards(events, home);
            awayCards += cards(events, away);
        }
        double ratio = (double) homeCards / awayCards;
        assertTrue(ratio > 0.9 && ratio < 1.1, "Kartlar dengeli olmali, oran: " + ratio);
    }

    private static void assertSideConsistent(MatchTeamStats stats, int goals, int cards) {
        assertTrue(stats.getShots() >= stats.getShotsOnTarget(), "Sut >= isabetli sut");
        assertTrue(stats.getShotsOnTarget() >= goals, "Isabetli sut >= gol");
        assertTrue(stats.getFouls() >= cards, "Faul >= kart");
        assertTrue(stats.getPossession() >= 30 && stats.getPossession() <= 70);
        assertTrue(stats.getCorners() >= 0 && stats.getOffsides() >= 0 && stats.getSaves() >= 0);
    }

    private MatchDetailGenerator.GeneratedDetail generate(Match match) {
        ScoreSimulator.ExpectedGoals expected = scoreSimulator.expectedGoals(
                ThreadLocalRandom.current().nextInt(1, 101), 50, ThreadLocalRandom.current().nextInt(1, 101), 50);
        return generator.generate(match, homeSquad, awaySquad, expected);
    }

    private Match randomPlayedMatch() {
        return Match.builder()
                .homeTeam(home)
                .awayTeam(away)
                .homeScore(ThreadLocalRandom.current().nextInt(0, 7))
                .awayScore(ThreadLocalRandom.current().nextInt(0, 7))
                .build();
    }

    private static int cards(List<MatchEvent> events, Team team) {
        return (int) events.stream()
                .filter(event -> event.getTeam() == team && (event.getType() == MatchEventType.YELLOW_CARD
                        || event.getType() == MatchEventType.RED_CARD))
                .count();
    }

    private static List<Player> withIds(List<Player> squad, long firstId) {
        IntStream.range(0, squad.size()).forEach(i -> squad.get(i).setId(firstId + i));
        return squad;
    }
}
