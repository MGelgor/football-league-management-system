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
        List<List<RoundRobinScheduler.Pairing>> weeks = scheduler.generateDoubleRoundRobin(teamIds(18));

        assertEquals(34, weeks.size());
        weeks.forEach(this::assertNoTeamPlaysTwice);

        Map<String, Set<String>> directedByPair = new HashMap<>();
        for (List<RoundRobinScheduler.Pairing> week : weeks) {
            for (RoundRobinScheduler.Pairing pairing : week) {
                directedByPair.computeIfAbsent(normalizedKey(pairing.homeTeamId(), pairing.awayTeamId()),
                        key -> new HashSet<>()).add(pairing.homeTeamId() + ">" + pairing.awayTeamId());
            }
        }
        assertEquals(18 * 17 / 2, directedByPair.size());
        directedByPair.values().forEach(directed -> assertEquals(2, directed.size(),
                "Her ikili iki kez, ev sahibi/deplasman ters olacak sekilde oynamali"));
    }

    @Test
    void evSahibiDeplasmanDengeliDagilir() {
        for (int n : List.of(18, 20)) {
            List<Long> teamIds = teamIds(n);
            List<List<RoundRobinScheduler.Pairing>> weeks = scheduler.generateDoubleRoundRobin(teamIds);

            for (Long teamId : teamIds) {
                List<Boolean> venues = new ArrayList<>();
                for (List<RoundRobinScheduler.Pairing> week : weeks) {
                    for (RoundRobinScheduler.Pairing pairing : week) {
                        if (pairing.homeTeamId().equals(teamId)) {
                            venues.add(true);
                        } else if (pairing.awayTeamId().equals(teamId)) {
                            venues.add(false);
                        }
                    }
                }
                assertEquals(2 * (n - 1), venues.size());
                assertEquals(n - 1, venues.stream().filter(home -> home).count(), "Yarisi ev sahibi olmali");

                int breaks = 0;
                for (int i = 1; i < venues.size(); i++) {
                    if (venues.get(i).equals(venues.get(i - 1))) {
                        breaks++;
                    }
                }
                assertTrue(breaks <= 2, "Takim " + teamId + " icin ardisik ayni saha sayisi fazla: " + breaks);
            }
        }
    }

    @Test
    void ayniRakiplerIkiHaftaArdisikKarsilasmaz() {
        List<List<RoundRobinScheduler.Pairing>> weeks = scheduler.generateDoubleRoundRobin(teamIds(18));

        for (int i = 1; i < weeks.size(); i++) {
            Set<String> previous = weeks.get(i - 1).stream()
                    .map(p -> normalizedKey(p.homeTeamId(), p.awayTeamId())).collect(Collectors.toSet());
            for (RoundRobinScheduler.Pairing pairing : weeks.get(i)) {
                assertTrue(!previous.contains(normalizedKey(pairing.homeTeamId(), pairing.awayTeamId())),
                        "Hafta " + (i + 1) + " bir onceki haftanin rovansi olmamali");
            }
        }
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
