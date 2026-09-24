package com.footballleague.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.footballleague.dto.StandingResponse;

class StandingsCalculatorTest {

    private final StandingsCalculator calculator = new StandingsCalculator();

    @Test
    void puanaGoreSiralar() {
        List<StandingsCalculator.TeamInfo> teams = List.of(
                new StandingsCalculator.TeamInfo(1L, "A"),
                new StandingsCalculator.TeamInfo(2L, "B"),
                new StandingsCalculator.TeamInfo(3L, "C"));

        List<StandingsCalculator.MatchResult> results = List.of(
                new StandingsCalculator.MatchResult(1L, 2, 2L, 0), // A 2-0 B
                new StandingsCalculator.MatchResult(1L, 3, 3L, 1), // A 3-1 C
                new StandingsCalculator.MatchResult(2L, 1, 3L, 0)  // B 1-0 C
        );

        List<StandingResponse> standings = calculator.calculate(teams, results);

        assertEquals("A", standings.get(0).teamName());
        assertEquals(6, standings.get(0).points());
        assertEquals("B", standings.get(1).teamName());
        assertEquals(3, standings.get(1).points());
        assertEquals("C", standings.get(2).teamName());
        assertEquals(0, standings.get(2).points());
    }

    @Test
    void esitPuandaAverajaGoreSiralar() {
        List<StandingsCalculator.TeamInfo> teams = List.of(
                new StandingsCalculator.TeamInfo(1L, "Takim1"),
                new StandingsCalculator.TeamInfo(2L, "Takim2"),
                new StandingsCalculator.TeamInfo(3L, "Takim3"),
                new StandingsCalculator.TeamInfo(4L, "Takim4"));

        List<StandingsCalculator.MatchResult> results = List.of(
                new StandingsCalculator.MatchResult(1L, 3, 3L, 0), // Takim1 3-0 Takim3 (averaj +3)
                new StandingsCalculator.MatchResult(2L, 1, 4L, 0)  // Takim2 1-0 Takim4 (averaj +1)
        );

        List<StandingResponse> standings = calculator.calculate(teams, results);

        assertEquals(3, standings.get(0).points());
        assertEquals(3, standings.get(1).points());
        assertEquals("Takim1", standings.get(0).teamName(), "Ayni puanda daha iyi averaja sahip takim onde olmali");
        assertEquals("Takim2", standings.get(1).teamName());
        assertEquals(3, standings.get(0).goalDifference());
        assertEquals(1, standings.get(1).goalDifference());
    }

    @Test
    void esitPuanVeAverajdaAtilanGoleGoreSiralar() {
        List<StandingsCalculator.TeamInfo> teams = List.of(
                new StandingsCalculator.TeamInfo(1L, "Takim1"),
                new StandingsCalculator.TeamInfo(2L, "Takim2"),
                new StandingsCalculator.TeamInfo(3L, "Takim3"),
                new StandingsCalculator.TeamInfo(4L, "Takim4"));

        List<StandingsCalculator.MatchResult> results = List.of(
                new StandingsCalculator.MatchResult(1L, 3, 3L, 1), // Takim1 3-1 Takim3 (averaj +2, atilan 3)
                new StandingsCalculator.MatchResult(2L, 2, 4L, 0)  // Takim2 2-0 Takim4 (averaj +2, atilan 2)
        );

        List<StandingResponse> standings = calculator.calculate(teams, results);

        assertEquals(3, standings.get(0).points());
        assertEquals(3, standings.get(1).points());
        assertEquals(2, standings.get(0).goalDifference());
        assertEquals(2, standings.get(1).goalDifference());
        assertEquals("Takim1", standings.get(0).teamName(),
                "Ayni puan/averajda daha fazla gol atan takim onde olmali");
    }

    @Test
    void hicMacOynamamisTakimSifirlarlaTabloyaGirer() {
        List<StandingsCalculator.TeamInfo> teams = List.of(new StandingsCalculator.TeamInfo(1L, "YeniTakim"));

        List<StandingResponse> standings = calculator.calculate(teams, List.of());

        assertEquals(1, standings.size());
        assertEquals(0, standings.get(0).played());
        assertEquals(0, standings.get(0).points());
    }
}
