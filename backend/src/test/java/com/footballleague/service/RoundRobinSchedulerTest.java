package com.footballleague.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;

class RoundRobinSchedulerTest {

    private final RoundRobinScheduler scheduler = new RoundRobinScheduler();

    @Test
    void herTakimHerRakipleTamBirKezEslesirTekDevrede() {
        List<Long> teamIds = teamIds(18);

        List<List<RoundRobinScheduler.Pairing>> weeks = scheduler.generateSingleRoundRobin(teamIds);

        assertEquals(17, weeks.size(), "18 takim icin 17 hafta olmali");

        Set<String> seenPairs = new HashSet<>();
        for (List<RoundRobinScheduler.Pairing> week : weeks) {
            assertEquals(9, week.size(), "Her haftada 9 mac olmali");
            assertNoTeamPlaysTwice(week);

            for (RoundRobinScheduler.Pairing pairing : week) {
                String key = normalizedKey(pairing.homeTeamId(), pairing.awayTeamId());
                assertTrue(seenPairs.add(key), "Ayni eslesme birden fazla kez uretilmis: " + key);
            }
        }

        assertEquals(18L * 17 / 2, seenPairs.size(), "Tum ikili eslesmeler tam bir kez olusmali");
    }

    @Test
    void ciftDevredeHerTakimHerRakipleTamIkiKezEslesirEvSahibiDeplasmanTersCevrilerek() {
        List<Long> teamIds = teamIds(18);
        List<List<RoundRobinScheduler.Pairing>> firstLeg = scheduler.generateSingleRoundRobin(teamIds);

        // FixtureService'in yaptigi gibi: rovans haftalarinda ev sahibi/deplasman ters cevrilir
        List<RoundRobinScheduler.Pairing> allMatches = new ArrayList<>();
        firstLeg.forEach(allMatches::addAll);
        firstLeg.forEach(week -> week.forEach(
                p -> allMatches.add(new RoundRobinScheduler.Pairing(p.awayTeamId(), p.homeTeamId()))));

        assertEquals(18 * 17, allMatches.size(), "N*(N-1) toplam mac olmali");

        Map<String, Integer> normalizedCounts = new HashMap<>();
        for (RoundRobinScheduler.Pairing pairing : allMatches) {
            normalizedCounts.merge(normalizedKey(pairing.homeTeamId(), pairing.awayTeamId()), 1, Integer::sum);
        }
        normalizedCounts.values()
                .forEach(count -> assertEquals(2, count, "Her ikili tam olarak 2 kez (gidis-donus) oynamali"));
    }

    @Test
    void takimSayisiTekIseHataFirlatir() {
        assertThrows(IllegalArgumentException.class, () -> scheduler.generateSingleRoundRobin(teamIds(17)));
    }

    @Test
    void farkliTakimSayilariIcinHaftaVeMacSayisiDogru() {
        for (int n : List.of(4, 6, 20, 30)) {
            List<List<RoundRobinScheduler.Pairing>> weeks = scheduler.generateSingleRoundRobin(teamIds(n));
            assertEquals(n - 1, weeks.size());
            weeks.forEach(week -> assertEquals(n / 2, week.size()));
            weeks.forEach(this::assertNoTeamPlaysTwice);
        }
    }

    private void assertNoTeamPlaysTwice(List<RoundRobinScheduler.Pairing> week) {
        Set<Long> teamsThisWeek = new HashSet<>();
        for (RoundRobinScheduler.Pairing pairing : week) {
            assertTrue(teamsThisWeek.add(pairing.homeTeamId()),
                    "Takim ayni hafta icinde iki kez oynuyor: " + pairing.homeTeamId());
            assertTrue(teamsThisWeek.add(pairing.awayTeamId()),
                    "Takim ayni hafta icinde iki kez oynuyor: " + pairing.awayTeamId());
        }
    }

    private String normalizedKey(Long a, Long b) {
        return a < b ? a + "-" + b : b + "-" + a;
    }

    private List<Long> teamIds(int count) {
        return LongStream.rangeClosed(1, count).boxed().collect(Collectors.toList());
    }
}
