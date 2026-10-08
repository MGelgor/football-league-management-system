package com.footballleague.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.footballleague.dto.SeasonResponse;
import com.footballleague.dto.SeasonResultResponse;
import com.footballleague.dto.StandingResponse;
import com.footballleague.entity.Competition;
import com.footballleague.entity.Match;
import com.footballleague.entity.MatchWeek;
import com.footballleague.entity.Season;
import com.footballleague.entity.SeasonTeamChange;
import com.footballleague.entity.Team;
import com.footballleague.entity.TeamChangeType;
import com.footballleague.exception.FixtureNotGeneratedException;
import com.footballleague.repository.MatchRepository;
import com.footballleague.repository.MatchWeekRepository;
import com.footballleague.repository.SeasonRepository;
import com.footballleague.repository.SeasonTeamChangeRepository;

@ExtendWith(MockitoExtension.class)
class SeasonServiceTest {

    private static final Long SEASON_ID = 5L;

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private MatchWeekRepository matchWeekRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchSimulationService matchSimulationService;

    @Mock
    private StandingsService standingsService;

    @Mock
    private SeasonTeamChangeRepository seasonTeamChangeRepository;

    @InjectMocks
    private SeasonService seasonService;

    @Test
    void fiksturYoksaHataFirlatir() {
        when(seasonRepository.findTopByOrderBySeasonNumberDesc()).thenReturn(Optional.empty());

        assertThrows(FixtureNotGeneratedException.class, () -> seasonService.playRemainingSeason());
        verifyNoInteractions(matchSimulationService);
    }

    @Test
    void sadeceOynanmamisHaftalarOynatilirVeLiderSampiyonOlur() {
        when(seasonRepository.findTopByOrderBySeasonNumberDesc())
                .thenReturn(Optional.of(Season.builder().id(SEASON_ID).seasonNumber(1).build()));
        MatchWeek playedWeek = MatchWeek.builder().id(1L).weekNumber(1).build();
        MatchWeek unplayedWeek = MatchWeek.builder().id(2L).weekNumber(2).build();
        when(matchWeekRepository.findBySeasonIdAndCompetitionOrderByWeekNumber(SEASON_ID, Competition.LEAGUE))
                .thenReturn(List.of(playedWeek, unplayedWeek));
        when(matchRepository.findByMatchWeekIdOrderById(1L))
                .thenReturn(List.of(Match.builder().homeScore(1).awayScore(0).build()));
        when(matchRepository.findByMatchWeekIdOrderById(2L)).thenReturn(List.of(Match.builder().build()));

        List<StandingResponse> standings = List.of(
                new StandingResponse(1, 10L, "Lider", 2, 2, 0, 0, 4, 1, 3, 6, 0),
                new StandingResponse(2, 20L, "Ikinci", 2, 1, 0, 1, 2, 2, 0, 3, 0));
        when(standingsService.getStandings(SEASON_ID)).thenReturn(standings);

        SeasonResultResponse result = seasonService.playRemainingSeason();

        verify(matchSimulationService, never()).playWeek(1, false);
        verify(matchSimulationService).playWeek(2, false);
        assertEquals("Lider", result.championName());
        assertEquals(6, result.championPoints());
        assertEquals(standings, result.finalStandings());
    }

    @Test
    void sezonListesiSampiyonKupaDusenCikanVeMacSayilariylaDoner() {
        Team champion = Team.builder().id(9L).name("Sampiyon FK").build();
        Team cupWinner = Team.builder().id(8L).name("Kupa FK").build();
        Team relegated = Team.builder().id(7L).name("Dusen FK").build();
        Team promoted = Team.builder().id(6L).name("Cikan FK").build();
        Season finished = Season.builder().id(1L).seasonNumber(1).finished(true).champion(champion)
                .cupWinner(cupWinner).build();
        Season current = Season.builder().id(2L).seasonNumber(2).build();
        when(seasonRepository.findAllByOrderBySeasonNumberDesc()).thenReturn(List.of(current, finished));
        when(seasonTeamChangeRepository.findAllWithTeams()).thenReturn(List.of(
                SeasonTeamChange.builder().season(finished).team(relegated).type(TeamChangeType.RELEGATED).build(),
                SeasonTeamChange.builder().season(finished).team(promoted).type(TeamChangeType.PROMOTED).build()));
        when(matchRepository.countLeagueMatches(2L)).thenReturn(306L);
        when(matchRepository.countPlayedLeagueMatches(2L)).thenReturn(18L);
        when(matchRepository.countLeagueMatches(1L)).thenReturn(306L);
        when(matchRepository.countPlayedLeagueMatches(1L)).thenReturn(306L);

        List<SeasonResponse> seasons = seasonService.getSeasons();

        assertEquals(new SeasonResponse(2L, 2, false, null, null, 306, 18, null, null, List.of(), List.of()),
                seasons.get(0));
        assertEquals(new SeasonResponse(1L, 1, true, 9L, "Sampiyon FK", 306, 306, 8L, "Kupa FK",
                List.of(new SeasonResponse.TeamRef(7L, "Dusen FK")), List.of(new SeasonResponse.TeamRef(6L, "Cikan FK"))),
                seasons.get(1));
    }
}
