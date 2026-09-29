package com.footballleague.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.footballleague.entity.Team;

@DataJpaTest
class TeamRepositoryTest {

    @Autowired
    private TeamRepository teamRepository;

    @Test
    void isimAramasiBuyukKucukHarfeDuyarsizdir() {
        Team saved = teamRepository.save(team("Galatasaray"));

        assertTrue(teamRepository.existsByNameIgnoreCase("GALATASARAY"));
        assertFalse(teamRepository.existsByNameIgnoreCase("Fenerbahce"));
        assertEquals(saved.getId(), teamRepository.findByNameIgnoreCase("galatasaray").orElseThrow().getId());
    }

    @Test
    void veritabaniAyniIsimdeIkinciTakimaIzinVermez() {
        teamRepository.saveAndFlush(team("Galatasaray"));

        assertThrows(DataIntegrityViolationException.class, () -> teamRepository.saveAndFlush(team("Galatasaray")));
    }

    private Team team(String name) {
        return Team.builder().name(name).foundedYear(1905).colors("Sari-Kirmizi").strength(50).morale(50).build();
    }
}
