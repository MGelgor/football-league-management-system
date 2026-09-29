package com.footballleague.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import com.footballleague.dto.SeasonResultResponse;
import com.footballleague.dto.StandingResponse;
import com.footballleague.entity.Match;
import com.footballleague.entity.MatchWeek;
import com.footballleague.exception.FixtureNotGeneratedException;
import com.footballleague.repository.MatchRepository;
import com.footballleague.repository.MatchWeekRepository;

@ExtendWith(MockitoExtension.class)
class SeasonServiceTest {

    private static final Sort BY_WEEK_NUMBER = Sort.by("weekNumber");

    @Mock
    private MatchWeekRepository matchWeekRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchSimulationService matchSimulationService;

    @Mock
    private StandingsService standingsService;

    @InjectMocks
    private SeasonService seasonService;

    @Test
    void fiksturYoksaHataFirlatir() {
        when(matchWeekRepository.findAll(BY_WEEK_NUMBER)).thenReturn(List.of());

        assertThrows(FixtureNotGeneratedException.class, () -> seasonService.playRemainingSeason());
        verifyNoInteractions(matchSimulationService);
    }

    @Test
    void sadeceOynanmamisHaftalarOynatilirVeLiderSampiyonOlur() {
        MatchWeek playedWeek = MatchWeek.builder().id(1L).weekNumber(1).build();
        MatchWeek unplayedWeek = MatchWeek.builder().id(2L).weekNumber(2).build();
        when(matchWeekRepository.findAll(BY_WEEK_NUMBER)).thenReturn(List.of(playedWeek, unplayedWeek));
        when(matchRepository.findByMatchWeekIdOrderById(1L))
                .thenReturn(List.of(Match.builder().homeScore(1).awayScore(0).build()));
        when(matchRepository.findByMatchWeekIdOrderById(2L)).thenReturn(List.of(Match.builder().build()));

        List<StandingResponse> standings = List.of(
                new StandingResponse(1, 10L, "Lider", 2, 2, 0, 0, 4, 1, 3, 6),
                new StandingResponse(2, 20L, "Ikinci", 2, 1, 0, 1, 2, 2, 0, 3));
        when(standingsService.getStandings()).thenReturn(standings);

        SeasonResultResponse result = seasonService.playRemainingSeason();

        verify(matchSimulationService, never()).playWeek(1);
        verify(matchSimulationService).playWeek(2);
        assertEquals("Lider", result.championName());
        assertEquals(6, result.championPoints());
        assertEquals(standings, result.finalStandings());
    }
}
