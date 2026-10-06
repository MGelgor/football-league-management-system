package com.footballleague.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.footballleague.dto.TeamRequest;

class RandomTeamGeneratorTest {

    private final RandomTeamGenerator generator = new RandomTeamGenerator();

    @Test
    void istenenSayidaTekrarsizVeVarOlanlarlaCakismayanTakimUretir() {
        List<TeamRequest> teams = generator.generate(50, Set.of("ADANASPOR", "Bursa FK"));

        assertEquals(50, teams.size());
        Set<String> names = teams.stream().map(TeamRequest::name).collect(Collectors.toSet());
        assertEquals(50, names.size(), "Adlar tekrarsiz olmali");
        assertTrue(!names.contains("Adanaspor") && !names.contains("Bursa FK"),
                "Var olan adlar buyuk/kucuk harf farkiyla da tekrar uretilmemeli");
    }

    @Test
    void kurulusYiliVeRenklerGecerli() {
        for (TeamRequest team : generator.generate(50, Set.of())) {
            assertTrue(team.foundedYear() >= 1900 && team.foundedYear() <= 2020);
            String[] colors = team.colors().split("-");
            assertEquals(2, colors.length);
            assertTrue(!colors[0].equals(colors[1]), "Iki renk farkli olmali: " + team.colors());
        }
    }

    @Test
    void azSayidaTakimdaHerSehirdenEnFazlaBirTakimOlur() {
        List<TeamRequest> teams = generator.generate(20, Set.of());
        // Ad = sehir + ek; ilk kelime (ya da 'spor' eki oncesi) sehirdir
        long distinctCities = teams.stream()
                .map(team -> team.name().split(" ")[0].replaceAll("spor$", ""))
                .distinct()
                .count();
        assertEquals(20, distinctCities);
    }

    @Test
    void adayKalmazsaAnlamliHataVerir() {
        assertThrows(IllegalArgumentException.class, () -> generator.generate(1000, Set.of()));
    }
}
