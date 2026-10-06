package com.footballleague.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.footballleague.entity.CupRound;
import com.footballleague.entity.Match;
import com.footballleague.entity.Team;

class CupServiceTest {

    @Test
    void penaltiAtislariHepKazananlaBiterVeGecerliSkorUretir() {
        for (int i = 0; i < 5000; i++) {
            int[] shootout = CupService.penaltyShootout();
            int home = shootout[0];
            int away = shootout[1];
            assertTrue(home != away, "Penaltilar beraberlikle bitemez");
            if (Math.max(home, away) > 5) {
                assertEquals(1, Math.abs(home - away), "Seri penaltilarda fark 1 olur: " + home + "-" + away);
            }
        }
    }

    @Test
    void penaltiyiKazananMacinKazananidir() {
        Team home = Team.builder().id(1L).build();
        Team away = Team.builder().id(2L).build();
        Match match = Match.builder().homeTeam(home).awayTeam(away).homeScore(1).awayScore(1)
                .homePenalties(3).awayPenalties(4).build();

        assertEquals(away, match.winner());
    }

    @Test
    void turlarCeyrekYariVeFinalSirasiyla() {
        assertEquals(CupRound.SEMI_FINAL, CupRound.QUARTER_FINAL.next());
        assertEquals(CupRound.FINAL, CupRound.SEMI_FINAL.next());
        assertEquals(null, CupRound.FINAL.next());
        assertEquals(4 + 2 + 1, CupRound.QUARTER_FINAL.matchCount() + CupRound.SEMI_FINAL.matchCount()
                + CupRound.FINAL.matchCount());
    }
}
