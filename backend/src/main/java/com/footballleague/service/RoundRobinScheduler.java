package com.footballleague.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Round-robin fikstür üretir: "circle method".
 * N takım verildiğinde tek devrede N-1 hafta üretir, her hafta N/2 maç içerir,
 * her takım diğer her takımla tam olarak bir kez eşleşir.
 * Ev sahibi/deplasman dağılımı dengelidir: her takım neredeyse her hafta venue değiştirir
 * (tek devrede takım başına en fazla 1 kırılma — ardışık iki ev/deplasman maçı).
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
                // Sabit takım her hafta venue değiştirir; diğer çiftlerde venue çemberdeki sıraya bağlıdır.
                // Çember her hafta bir kaydığı için takımın sırası ve dolayısıyla venue'su da dönüşümlü değişir.
                boolean homeIsA = i == 0 ? round % 2 == 0 : i % 2 == 0;
                weekPairings.add(homeIsA ? new Pairing(teamA, teamB) : new Pairing(teamB, teamA));
            }
            weeks.add(weekPairings);

            rotating.add(0, rotating.remove(rotating.size() - 1));
        }
        return weeks;
    }

    /**
     * Çift devreli fikstür: ilk devre + rövanş devresi (2(N-1) hafta). Rövanşta ev sahibi/deplasman ters çevrilir.
     * Rövanş devresi 2. haftanın rövanşıyla başlar ve 1. haftanın rövanşıyla biter; böylece devre arasında
     * takımlar aynı venue'da ardışık oynamaz ve 17. haftanın rakibiyle 18. haftada tekrar karşılaşmaz.
     */
    public List<List<Pairing>> generateDoubleRoundRobin(List<Long> teamIds) {
        List<List<Pairing>> firstLeg = generateSingleRoundRobin(teamIds);
        List<List<Pairing>> weeks = new ArrayList<>(firstLeg);
        for (int i = 1; i <= firstLeg.size(); i++) {
            weeks.add(reversed(firstLeg.get(i % firstLeg.size())));
        }
        return weeks;
    }

    private List<Pairing> reversed(List<Pairing> week) {
        return week.stream().map(pairing -> new Pairing(pairing.awayTeamId(), pairing.homeTeamId())).toList();
    }

    public record Pairing(Long homeTeamId, Long awayTeamId) {
    }
}
