package com.footballleague.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.footballleague.dto.PlayerRequest;
import com.footballleague.dto.TeamRequest;
import com.footballleague.dto.TeamResponse;
import com.footballleague.entity.Player;
import com.footballleague.entity.Position;
import com.footballleague.entity.Season;
import com.footballleague.entity.Team;
import com.footballleague.exception.BigFourLockedException;
import com.footballleague.exception.DuplicateTeamNameException;
import com.footballleague.exception.TeamNotFoundException;
import com.footballleague.exception.TeamsLockedException;
import com.footballleague.repository.MatchRepository;
import com.footballleague.repository.PlayerRepository;
import com.footballleague.repository.SeasonRepository;
import com.footballleague.repository.TeamRepository;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private FileStorageService fileStorageService;

    private TeamService teamService;

    @BeforeEach
    void setUp() {
        teamService = new TeamService(teamRepository, matchRepository, seasonRepository, playerRepository,
                fileStorageService, new SquadGenerator(), new RandomTeamGenerator());
    }

    @Test
    void yeniTakimNormalGucBandindaBaslangicMoraliVe18KisilikKadroylaKaydedilir() {
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeamResponse response = teamService.createTeam(new TeamRequest("Yeni Takim", 1990, "Mavi-Beyaz"));

        assertEquals(Team.INITIAL_MORALE, response.morale());
        assertFalse(response.bigFour());
        assertTrue(response.strength() >= 20 && response.strength() <= 80,
                "Normal takim gucu 20-80 araliginda baslamali: " + response.strength());
        assertEquals(0, response.seasonStrengthChange());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Player>> squadCaptor = ArgumentCaptor.forClass(List.class);
        verify(playerRepository).saveAll(squadCaptor.capture());
        assertEquals(18, squadCaptor.getValue().size());
    }

    @Test
    void rastgeleTakimlarVarOlanAdlarlaCakismadanKadrolarylaEklenir() {
        when(teamRepository.findByActiveTrue()).thenReturn(List.of(team(1L, "Adanaspor")));
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<TeamResponse> created = teamService.createRandomTeams(5);

        assertEquals(5, created.size());
        assertTrue(created.stream().noneMatch(team -> team.name().equals("Adanaspor")));
        assertTrue(created.stream().allMatch(team -> !team.bigFour() && team.strength() >= 20 && team.strength() <= 80));
        verify(playerRepository, times(5)).saveAll(any());
    }

    @Test
    void sezonDevamEderkenRastgeleTakimEklenemez() {
        givenSeason(false);

        assertThrows(TeamsLockedException.class, () -> teamService.createRandomTeams(3));
        verify(teamRepository, never()).save(any());
    }

    @Test
    void olustururkenEklenenOyuncularKadroyaGirerEksiklerTamamlanir() {
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));
        List<PlayerRequest> players = List.of(
                new PlayerRequest("Kral Golcu", Position.FORWARD, 9, 100, 25),
                new PlayerRequest("Usta Kaleci", Position.GOALKEEPER, 1, 80, 25));

        teamService.createTeam(new TeamRequest("Oyunculu FK", 1990, "Mor", players));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Player>> squadCaptor = ArgumentCaptor.forClass(List.class);
        verify(playerRepository).saveAll(squadCaptor.capture());
        List<Player> squad = squadCaptor.getValue();
        assertEquals(18, squad.size());
        Player golcu = squad.stream().filter(p -> p.getName().equals("Kral Golcu")).findFirst().orElseThrow();
        assertEquals(Position.FORWARD, golcu.getPosition());
        assertEquals(100, golcu.getStrength());
        assertEquals(9, golcu.getShirtNumber());
    }

    @Test
    void olustururkenAyniFormaNumarasiVerilirseTakimKaydedilmez() {
        List<PlayerRequest> players = List.of(
                new PlayerRequest("A", Position.FORWARD, 7, 50, 25),
                new PlayerRequest("B", Position.DEFENDER, 7, 50, 25));

        assertThrows(IllegalArgumentException.class,
                () -> teamService.createTeam(new TeamRequest("Cakisma FK", 1990, "Mor", players)));
        verify(teamRepository, never()).save(any());
    }

    @Test
    void ayniIsimdeAktifTakimVarsaEklenmez() {
        when(teamRepository.existsByNameIgnoreCaseAndActiveTrue("Galatasaray")).thenReturn(true);

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
    void sezonDevamEderkenTakimEklenemez() {
        givenSeason(false);

        assertThrows(TeamsLockedException.class,
                () -> teamService.createTeam(new TeamRequest("Gec Kalan FK", 2000, "Gri")));
        verify(teamRepository, never()).save(any());
    }

    @Test
    void sezonBittiktenSonraTakimEklenebilir() {
        givenSeason(true);
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

        teamService.createTeam(new TeamRequest("Yeni Sezon FK", 2000, "Gri"));

        verify(teamRepository).save(any(Team.class));
    }

    @Test
    void sezonDevamEderkenTakimSilinemez() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team(1L, "A")));
        givenSeason(false);

        assertThrows(TeamsLockedException.class, () -> teamService.deleteTeam(1L));
        verify(teamRepository, never()).delete(any());
    }

    @Test
    void macGecmisiOlmayanTakimKadrosuylaSilinir() {
        Team team = team(1L, "A");
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

        teamService.deleteTeam(1L);

        verify(playerRepository).deleteByTeamId(1L);
        verify(teamRepository).delete(team);
    }

    @Test
    void macGecmisiOlanTakimSilinmezArsivlenir() {
        Team team = team(1L, "A");
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(matchRepository.existsByHomeTeamIdOrAwayTeamId(1L, 1L)).thenReturn(true);

        teamService.deleteTeam(1L);

        assertFalse(team.isActive());
        verify(teamRepository, never()).delete(any());
    }

    @Test
    void arsivlenmisTakimGoruntulenebilirAmaSilinemezOlmayanTakimBulunamaz() {
        Team archived = team(2L, "Eski FK");
        archived.setActive(false);
        when(teamRepository.findById(99L)).thenReturn(Optional.empty());
        when(teamRepository.findById(2L)).thenReturn(Optional.of(archived));

        assertThrows(TeamNotFoundException.class, () -> teamService.deleteTeam(99L));
        assertFalse(teamService.getTeam(2L).active(), "Kume dusen / silinen takimin sayfasi acilabilmeli");
        assertThrows(TeamNotFoundException.class, () -> teamService.deleteTeam(2L));
    }

    @Test
    void dortBuyuklerDuzenlenemezVeSilinemez() {
        Team galatasaray = team(1L, "Galatasaray");
        galatasaray.setBigFour(true);
        when(teamRepository.findById(1L)).thenReturn(Optional.of(galatasaray));

        assertThrows(BigFourLockedException.class,
                () -> teamService.updateTeam(1L, new TeamRequest("Galatasaray SK", 1905, "Sari-Kirmizi")));
        assertThrows(BigFourLockedException.class, () -> teamService.deleteTeam(1L));
        verify(teamRepository, never()).save(any());
        verify(teamRepository, never()).delete(any());
    }

    @Test
    void acilistaDortBuyuklerYuksekGucleOlusturulur() {
        List<Team> saved = new ArrayList<>();
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
            Team team = invocation.getArgument(0);
            saved.add(team);
            return team;
        });

        teamService.ensureBigFourAndSquads();

        assertEquals(List.of("Galatasaray", "Fenerbahçe", "Beşiktaş", "Trabzonspor"),
                saved.stream().map(Team::getName).toList());
        assertTrue(saved.stream().allMatch(Team::isBigFour));
        assertTrue(saved.stream().allMatch(team -> team.getStrength() >= Team.BIG_FOUR_MIN_STRENGTH),
                "4 buyuklerin gucu 85 ve uzeri olmali");
    }

    @Test
    void ayniIsimdeNormalTakimVarsaDortBuyugeCevrilirGucuYukselir() {
        Team existing = team(1L, "Galatasaray");
        existing.setStrength(40);
        when(teamRepository.findByNameIgnoreCaseAndActiveTrue(any())).thenReturn(Optional.empty());
        when(teamRepository.findByNameIgnoreCaseAndActiveTrue("Galatasaray")).thenReturn(Optional.of(existing));
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

        teamService.ensureBigFourAndSquads();

        assertTrue(existing.isBigFour());
        assertEquals(Team.BIG_FOUR_MIN_STRENGTH, existing.getStrength());
    }

    @Test
    void takimKendiAdininYazimiDegistirilerekGuncellenebilir() {
        Team takim = team(1L, "Kartal FK");
        when(teamRepository.findById(1L)).thenReturn(Optional.of(takim));
        when(teamRepository.findByNameIgnoreCaseAndActiveTrue("KARTAL FK")).thenReturn(Optional.of(takim));
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeamResponse updated = teamService.updateTeam(1L, new TeamRequest("KARTAL FK", 1905, "Siyah"));

        assertEquals("KARTAL FK", updated.name());
        assertEquals("Siyah", updated.colors());
    }

    @Test
    void takimBaskaBirTakiminAdiniAlamaz() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team(1L, "Kartal FK")));
        when(teamRepository.findByNameIgnoreCaseAndActiveTrue("Martı FK")).thenReturn(Optional.of(team(2L, "Martı FK")));

        assertThrows(DuplicateTeamNameException.class,
                () -> teamService.updateTeam(1L, new TeamRequest("Martı FK", 1905, "Siyah")));
        verify(teamRepository, never()).save(any());
    }

    @Test
    void logoYoluUploadsAdresineCevrilir() {
        Team team = team(1L, "A");
        team.setLogoPath("logos/abc.png");
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

        assertEquals("/uploads/logos/abc.png", teamService.getTeam(1L).logoUrl());
    }

    private void givenSeason(boolean finished) {
        when(seasonRepository.findTopByOrderBySeasonNumberDesc())
                .thenReturn(Optional.of(Season.builder().id(1L).seasonNumber(1).finished(finished).build()));
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
