package com.footballleague.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.footballleague.entity.Match;
import com.footballleague.entity.MatchWeek;
import com.footballleague.entity.Season;
import com.footballleague.entity.Team;
import com.footballleague.exception.FixtureAlreadyGeneratedException;
import com.footballleague.exception.SeasonFinishedException;
import com.footballleague.repository.MatchAppearanceRepository;
import com.footballleague.repository.MatchEventRepository;
import com.footballleague.repository.MatchRepository;
import com.footballleague.repository.MatchTeamStatsRepository;
import com.footballleague.repository.MatchWeekRepository;
import com.footballleague.repository.PlayerRepository;
import com.footballleague.repository.SeasonRepository;
import com.footballleague.repository.TeamRepository;

@ExtendWith(MockitoExtension.class)
class FixtureServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private MatchWeekRepository matchWeekRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchEventRepository matchEventRepository;

    @Mock
    private MatchTeamStatsRepository matchTeamStatsRepository;

    @Mock
    private MatchAppearanceRepository matchAppearanceRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Captor
    private ArgumentCaptor<List<MatchWeek>> weeksCaptor;

    @Captor
    private ArgumentCaptor<List<Match>> matchesCaptor;

    private FixtureService fixtureService;

    @BeforeEach
    void setUp() {
        // Repository'ler sahte, fikstur algoritmasi gercek
        fixtureService = new FixtureService(teamRepository, seasonRepository, matchWeekRepository, matchRepository,
                matchEventRepository, matchTeamStatsRepository, matchAppearanceRepository, playerRepository,
                new RoundRobinScheduler(),
                new MatchMapper(new ScoreSimulator()));
    }

    @Test
    void devamEdenSezonVarkenYeniFiksturOlusturulmaz() {
        when(seasonRepository.findTopByOrderBySeasonNumberDesc()).thenReturn(Optional.of(season(1, false)));

        assertThrows(FixtureAlreadyGeneratedException.class, () -> fixtureService.generateFixture());
        verify(matchRepository, never()).saveAll(any());
    }

    @Test
    void enAz18TakimGerekir() {
        when(teamRepository.findByActiveTrue()).thenReturn(teams(16));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> fixtureService.generateFixture());
        assertTrue(exception.getMessage().contains("en az 18"), exception.getMessage());
        verify(matchRepository, never()).saveAll(any());
    }

    @Test
    void takimSayisiCiftOlmalidir() {
        when(teamRepository.findByActiveTrue()).thenReturn(teams(19));

        assertThrows(IllegalArgumentException.class, () -> fixtureService.generateFixture());
        verify(matchRepository, never()).saveAll(any());
    }

    @Test
    void bitenSezondanSonraBirSonrakiSezonNumarasiylaYeniSezonAcilirVeTakimlarSifirlanir() {
        List<Team> teams = teams(18);
        teams.getFirst().setMorale(90);
        teams.getFirst().setLastStrengthChange(3);
        when(seasonRepository.findTopByOrderBySeasonNumberDesc()).thenReturn(Optional.of(season(2, true)));
        when(teamRepository.findByActiveTrue()).thenReturn(teams);
        ArgumentCaptor<Season> seasonCaptor = ArgumentCaptor.forClass(Season.class);
        when(seasonRepository.save(seasonCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        fixtureService.generateFixture();

        assertEquals(3, seasonCaptor.getValue().getSeasonNumber());
        assertEquals(Team.INITIAL_MORALE, teams.getFirst().getMorale());
        assertEquals(0, teams.getFirst().getLastStrengthChange());
        assertEquals(teams.getFirst().getStrength(), teams.getFirst().getSeasonStartStrength());
    }

    @Test
    void onSekizTakimIcin34HaftaVe306MacKaydedilirRovanstaSahaTersCevrilir() {
        when(teamRepository.findByActiveTrue()).thenReturn(teams(18));
        when(seasonRepository.save(any(Season.class))).thenAnswer(invocation -> invocation.getArgument(0));

        fixtureService.generateFixture();

        verify(matchWeekRepository).saveAll(weeksCaptor.capture());
        verify(matchRepository).saveAll(matchesCaptor.capture());
        List<MatchWeek> weeks = weeksCaptor.getValue();
        List<Match> matches = matchesCaptor.getValue();

        assertEquals(IntStream.rangeClosed(1, 34).boxed().toList(),
                weeks.stream().map(MatchWeek::getWeekNumber).toList());
        assertTrue(weeks.stream().allMatch(week -> week.getSeason().getSeasonNumber() == 1),
                "Ilk sezon 1 numarali olmali ve tum haftalar ona bagli olmali");
        assertEquals(306, matches.size());
        assertTrue(matches.stream().noneMatch(Match::isPlayed), "Yeni fiksturde oynanmis mac olmamali");

        Map<Integer, Set<List<Long>>> pairingsByWeek = matches.stream().collect(Collectors.groupingBy(
                match -> match.getMatchWeek().getWeekNumber(),
                Collectors.mapping(match -> List.of(match.getHomeTeam().getId(), match.getAwayTeam().getId()),
                        Collectors.toSet())));

        for (int week = 1; week <= 17; week++) {
            Set<List<Long>> reversedFirstLeg = pairingsByWeek.get(week).stream()
                    .map(pairing -> List.of(pairing.get(1), pairing.get(0)))
                    .collect(Collectors.toSet());
            assertEquals(reversedFirstLeg, pairingsByWeek.get(week + 17),
                    "Hafta " + (week + 17) + ", hafta " + week + "'in ev sahibi/deplasman ters cevrilmis hali olmali");
        }
    }

    @Test
    void sifirlamaDevamEdenSezonuSilerGucVeMoraliSezonBasinaDondurur() {
        Season current = season(1, false);
        current.setId(7L);
        Team team = Team.builder().id(1L).strength(70).seasonStartStrength(60).morale(90).lastStrengthChange(2).build();
        when(seasonRepository.findTopByOrderBySeasonNumberDesc()).thenReturn(Optional.of(current));
        when(teamRepository.findByActiveTrue()).thenReturn(List.of(team));

        fixtureService.resetFixture();

        InOrder inOrder = inOrder(matchEventRepository, matchAppearanceRepository, matchTeamStatsRepository,
                matchRepository, matchWeekRepository, seasonRepository);
        inOrder.verify(matchEventRepository).deleteBySeasonId(7L);
        inOrder.verify(matchAppearanceRepository).deleteBySeasonId(7L);
        inOrder.verify(matchTeamStatsRepository).deleteBySeasonId(7L);
        inOrder.verify(matchRepository).deleteBySeasonId(7L);
        inOrder.verify(matchWeekRepository).deleteBySeasonId(7L);
        inOrder.verify(seasonRepository).delete(current);
        assertEquals(Team.INITIAL_MORALE, team.getMorale());
        assertEquals(60, team.getStrength());
        assertEquals(0, team.getLastStrengthChange());
    }

    @Test
    void tamamlanmisSezonSifirlanamaz() {
        when(seasonRepository.findTopByOrderBySeasonNumberDesc()).thenReturn(Optional.of(season(1, true)));

        assertThrows(SeasonFinishedException.class, () -> fixtureService.resetFixture());
        verify(matchRepository, never()).deleteBySeasonId(any());
    }

    private Season season(int number, boolean finished) {
        return Season.builder().seasonNumber(number).finished(finished).build();
    }

    private List<Team> teams(int count) {
        return LongStream.rangeClosed(1, count)
                .mapToObj(id -> Team.builder().id(id).name("Takim " + id).strength(50).morale(50).build())
                .toList();
    }
}
