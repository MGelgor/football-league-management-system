package com.footballleague.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.footballleague.entity.Formation;
import com.footballleague.entity.Match;
import com.footballleague.entity.Player;
import com.footballleague.entity.Position;
import com.footballleague.entity.Team;

/** Güçlü rakibe karşı moral bonusu ve maç içi yorgunluk. */
class MatchMomentumTest {

    @Test
    void gucluRakibeKarsiGalibiyetVeGolEkstraMoralVerir() {
        // Denk takımlar: ±10, beraberlikte değişim yok
        assertEquals(10, MatchSimulationService.moraleChange(70, 72, 2, 1));
        assertEquals(-10, MatchSimulationService.moraleChange(70, 72, 1, 2));
        assertEquals(0, MatchSimulationService.moraleChange(70, 72, 1, 1));
        // 12 güç üstün rakip: galibiyete +6, her gole +1 (en fazla 3)
        assertEquals(10 + 6 + 2, MatchSimulationService.moraleChange(70, 82, 2, 1));
        assertEquals(10 + 6 + 3, MatchSimulationService.moraleChange(70, 82, 5, 0));
        assertEquals(3 + 1, MatchSimulationService.moraleChange(70, 82, 1, 1));
        assertEquals(-10 + 1, MatchSimulationService.moraleChange(70, 82, 1, 3));
        // Favori zayıf rakibe kaybederse ek kayıp, berabere kalırsa yarısı; bonus en fazla 10
        assertEquals(-10 - 6, MatchSimulationService.moraleChange(82, 70, 0, 1));
        assertEquals(-3, MatchSimulationService.moraleChange(82, 70, 0, 0));
        assertEquals(10 + 10 + 1, MatchSimulationService.moraleChange(50, 90, 1, 0));
    }

    @Test
    void macIciYorgunlukDakikaVeYasaGoreArtar() {
        Player prime = player(1L, Position.MIDFIELDER, 26);
        Player veteran = player(2L, Position.MIDFIELDER, 33);
        Player young = player(3L, Position.MIDFIELDER, 19);
        assertEquals(4.0, MatchDetailGenerator.matchFatigue(prime, 45), 1e-9);
        assertEquals(8.0, MatchDetailGenerator.matchFatigue(prime, 90), 1e-9);
        assertEquals(0.0, MatchDetailGenerator.matchFatigue(prime, 0), 1e-9);
        assertTrue(MatchDetailGenerator.matchFatigue(veteran, 90) > MatchDetailGenerator.matchFatigue(prime, 90));
        assertTrue(MatchDetailGenerator.matchFatigue(young, 90) < MatchDetailGenerator.matchFatigue(prime, 90));
    }

    @Test
    void devreArasindaGirenDincYedeklerIkinciYariYorgunlugunuAzaltir() {
        Team home = Team.builder().id(1L).name("Ev").build();
        Team away = Team.builder().id(2L).name("Deplasman").build();
        List<Player> homeSquad = squad(100);
        List<Player> awaySquad = squad(200);
        Match match = Match.builder().homeTeam(home).awayTeam(away).build();
        MatchDetailGenerator.LiveMatch live = new MatchDetailGenerator().start(match,
                new MatchDetailGenerator.SideSetup(homeSquad, Formation.F442, homeSquad.subList(0, 11), null, false),
                new MatchDetailGenerator.SideSetup(awaySquad, Formation.F442, awaySquad.subList(0, 11), null, false));
        int onPitch = live.side(true).onPitch().size();

        // 45 dakika oynayan 26 yaşındaki oyuncu: ikinci yarı ortalaması (4 + 8) / 2 = 6
        double before = InteractiveMatchService.secondHalfFatigue(live, true);
        assertEquals(6.0, before, 0.001);
        for (int i = 0; i < 3; i++) {
            MatchDetailGenerator.SideView side = live.side(true);
            Player outgoing = side.onPitch().stream().filter(p -> p.getPosition() != Position.GOALKEEPER)
                    .filter(p -> live.minutesPlayed(p, 45) == 45).findFirst().orElseThrow();
            live.substitute(true, outgoing, side.bench().getFirst(), 46);
        }
        double after = InteractiveMatchService.secondHalfFatigue(live, true);
        // 3 dinç oyuncu: 46'da 0 → 90'da ~4, ortalama ~2
        assertTrue(after < before - 0.5, "Yedekler yorgunluğu azaltır: " + before + " → " + after);
        assertEquals(onPitch, live.side(true).onPitch().size());
        assertEquals(0, live.minutesPlayed(live.side(true).onPitch().getLast(), 45));
        assertEquals(6.0, InteractiveMatchService.secondHalfFatigue(live, false), 0.001);
    }

    private static List<Player> squad(long firstId) {
        Position[] positions = { Position.GOALKEEPER, Position.DEFENDER, Position.DEFENDER, Position.DEFENDER,
                Position.DEFENDER, Position.MIDFIELDER, Position.MIDFIELDER, Position.MIDFIELDER, Position.MIDFIELDER,
                Position.FORWARD, Position.FORWARD, Position.GOALKEEPER, Position.DEFENDER, Position.MIDFIELDER,
                Position.FORWARD, Position.MIDFIELDER };
        return IntStream.range(0, positions.length)
                .mapToObj(i -> player(firstId + i, positions[i], 26))
                .toList();
    }

    private static Player player(long id, Position position, int age) {
        return Player.builder().id(id).name("Oyuncu " + id).position(position).age(age).strength(70)
                .shirtNumber((int) (id % 100) + 1).active(true).build();
    }
}
