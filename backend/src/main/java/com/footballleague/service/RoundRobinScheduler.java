package com.footballleague.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Tek devreli (single round-robin) fikstür üretir: "circle method".
 * N takım verildiğinde N-1 hafta üretir, her hafta N/2 maç içerir,
 * her takım diğer her takımla tam olarak bir kez eşleşir.
 */
@Component
public class RoundRobinScheduler {

    public List<List<Pairing>> generateSingleRoundRobin(List<Long> teamIds) {
        int n = teamIds.size();
        if (n < 2 || n % 2 != 0) {
            throw new IllegalArgumentException("Takım sayısı en az 2 ve çift olmalı");
        }

        Long fixed = teamIds.get(0);
        List<Long> rotating = new ArrayList<>(teamIds.subList(1, n));

        List<List<Pairing>> weeks = new ArrayList<>();
        for (int round = 0; round < n - 1; round++) {
            List<Long> arrangement = new ArrayList<>();
            arrangement.add(fixed);
            arrangement.addAll(rotating);

            List<Pairing> weekPairings = new ArrayList<>();
            for (int i = 0; i < n / 2; i++) {
                Long teamA = arrangement.get(i);
                Long teamB = arrangement.get(n - 1 - i);
                boolean homeIsA = (round + i) % 2 == 0;
                weekPairings.add(homeIsA ? new Pairing(teamA, teamB) : new Pairing(teamB, teamA));
            }
            weeks.add(weekPairings);

            rotating.add(0, rotating.remove(rotating.size() - 1));
        }
        return weeks;
    }

    public record Pairing(Long homeTeamId, Long awayTeamId) {
    }
}
