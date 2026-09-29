package com.footballleague.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Year;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.footballleague.dto.TeamRequest;
import com.footballleague.dto.TeamResponse;
import com.footballleague.entity.Team;
import com.footballleague.exception.DuplicateTeamNameException;
import com.footballleague.exception.TeamNotFoundException;
import com.footballleague.exception.TeamsLockedException;
import com.footballleague.repository.MatchRepository;
import com.footballleague.repository.TeamRepository;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private TeamService teamService;

    @Test
    void yeniTakimBaslangicMoraliVeGecerliAraliktaGucleKaydedilir() {
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeamResponse response = teamService.createTeam(new TeamRequest("Yeni Takim", 1990, "Mavi-Beyaz"));

        assertEquals(Team.INITIAL_MORALE, response.morale());
        assertTrue(response.strength() >= 1 && response.strength() <= 100,
                "Guc 1-100 araliginda olmali: " + response.strength());
    }

    @Test
    void ayniIsimdeTakimVarsaEklenmez() {
        when(teamRepository.existsByNameIgnoreCase("Galatasaray")).thenReturn(true);

        assertThrows(DuplicateTeamNameException.class,
                () -> teamService.createTeam(new TeamRequest("Galatasaray", 1905, "Sari-Kirmizi")));
        verify(teamRepository, never()).save(any());
    }

    @Test
    void kurulusYiliGelecekteOlamaz() {
        int nextYear = Year.now().getValue() + 1;

        assertThrows(IllegalArgumentException.class,
                () -> teamService.createTeam(new TeamRequest("Gelecek FK", nextYear, "Mor")));
        verify(teamRepository, never()).save(any());
    }

    @Test
    void fiksturOlusturulduktanSonraTakimEklenemez() {
        when(matchRepository.count()).thenReturn(306L);

        assertThrows(TeamsLockedException.class,
                () -> teamService.createTeam(new TeamRequest("Gec Kalan FK", 2000, "Gri")));
        verify(teamRepository, never()).save(any());
    }

    @Test
    void fiksturOlusturulduktanSonraTakimSilinemez() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team(1L, "A")));
        when(matchRepository.count()).thenReturn(306L);

        assertThrows(TeamsLockedException.class, () -> teamService.deleteTeam(1L));
        verify(teamRepository, never()).delete(any());
    }

    @Test
    void fiksturYokkenTakimSilinir() {
        Team team = team(1L, "A");
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

        teamService.deleteTeam(1L);

        verify(teamRepository).delete(team);
    }

    @Test
    void olmayanTakimSilinemez() {
        when(teamRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(TeamNotFoundException.class, () -> teamService.deleteTeam(99L));
    }

    @Test
    void takimKendiAdininYazimiDegistirilerekGuncellenebilir() {
        Team galatasaray = team(1L, "Galatasaray");
        when(teamRepository.findById(1L)).thenReturn(Optional.of(galatasaray));
        when(teamRepository.findByNameIgnoreCase("GALATASARAY")).thenReturn(Optional.of(galatasaray));
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeamResponse updated = teamService.updateTeam(1L, new TeamRequest("GALATASARAY", 1905, "Sari-Kirmizi"));

        assertEquals("GALATASARAY", updated.name());
        assertEquals("Sari-Kirmizi", updated.colors());
    }

    @Test
    void takimBaskaBirTakiminAdiniAlamaz() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team(1L, "Galatasaray")));
        when(teamRepository.findByNameIgnoreCase("Fenerbahce")).thenReturn(Optional.of(team(2L, "Fenerbahce")));

        assertThrows(DuplicateTeamNameException.class,
                () -> teamService.updateTeam(1L, new TeamRequest("Fenerbahce", 1905, "Sari-Kirmizi")));
        verify(teamRepository, never()).save(any());
    }

    @Test
    void logoYoluUploadsAdresineCevrilir() {
        Team team = team(1L, "A");
        team.setLogoPath("logos/abc.png");
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

        assertEquals("/uploads/logos/abc.png", teamService.getTeam(1L).logoUrl());
    }

    private Team team(Long id, String name) {
        return Team.builder()
                .id(id)
                .name(name)
                .foundedYear(1900)
                .colors("Mavi")
                .strength(50)
                .morale(Team.INITIAL_MORALE)
                .build();
    }
}
