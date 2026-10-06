package com.footballleague.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.footballleague.entity.Team;

@DataJpaTest
class TeamRepositoryTest {

    @Autowired
    private TeamRepository teamRepository;

    @Test
    void isimAramasiBuyukKucukHarfeDuyarsizdir() {
        Team saved = teamRepository.save(team("Kartal FK"));

        assertTrue(teamRepository.existsByNameIgnoreCaseAndActiveTrue("KARTAL FK"));
        assertFalse(teamRepository.existsByNameIgnoreCaseAndActiveTrue("Marti FK"));
        assertEquals(saved.getId(), teamRepository.findByNameIgnoreCaseAndActiveTrue("kartal fk").orElseThrow().getId());
    }

    @Test
    void arsivlenmisTakimAktifListedeYokVeAdiTekrarKullanilabilir() {
        Team archived = team("Kartal FK");
        archived.setActive(false);
        teamRepository.saveAndFlush(archived);

        assertFalse(teamRepository.existsByNameIgnoreCaseAndActiveTrue("Kartal FK"));
        Team reused = teamRepository.saveAndFlush(team("Kartal FK"));

        assertEquals(List.of(reused.getId()),
                teamRepository.findByActiveTrue().stream().map(Team::getId).toList());
    }

    private Team team(String name) {
        return Team.builder().name(name).foundedYear(1905).colors("Siyah-Beyaz").strength(50).morale(50).build();
    }
}
