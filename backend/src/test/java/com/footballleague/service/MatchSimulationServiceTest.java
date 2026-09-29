package com.footballleague.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
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

import com.footballleague.dto.MatchResponse;
import com.footballleague.dto.MatchWeekResponse;
import com.footballleague.entity.Match;
import com.footballleague.entity.MatchWeek;
import com.footballleague.entity.Team;
import com.footballleague.exception.MatchWeekNotFoundException;
import com.footballleague.exception.WeekAlreadyPlayedException;
import com.footballleague.repository.MatchRepository;
import com.footballleague.repository.MatchWeekRepository;
import com.footballleague.repository.TeamRepository;
import com.footballleague.service.ScoreSimulator.SimulatedScore;

@ExtendWith(MockitoExtension.class)
class MatchSimulationServiceTest {

    private static final Long WEEK_ID = 10L;

    @Mock
    private MatchWeekRepository matchWeekRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private ScoreSimulator scoreSimulator;

    @InjectMocks
    private MatchSimulationService matchSimulationService;

    @Test
    void olmayanHaftaOynatilamaz() {
        when(matchWeekRepository.findByWeekNumber(99)).thenReturn(Optional.empty());

        assertThrows(MatchWeekNotFoundException.class, () -> matchSimulationService.playWeek(99));
    }

    @Test
    void oynanmisHaftaTekrarOynatilamaz() {
        Match played = match(team(1L, 50, 50), team(2L, 50, 50));
        played.setHomeScore(1);
        played.setAwayScore(0);
        givenWeekOneWithMatches(played);

        assertThrows(WeekAlreadyPlayedException.class, () -> matchSimulationService.playWeek(1));
        verifyNoInteractions(scoreSimulator);
    }

    @Test
    void simulatoreTakimlarinGucVeMoraliGonderilir() {
        givenWeekOneWithMatches(match(team(1L, 80, 70), team(2L, 40, 30)));
        when(scoreSimulator.simulate(80, 70, 40, 30)).thenReturn(new SimulatedScore(0, 0));

        matchSimulationService.playWeek(1);

        verify(scoreSimulator).simulate(80, 70, 40, 30);
    }

    @Test
    void skorlarKaydedilirVeMorallerSonucaGoreDegisir() {
        Team homeWinner = team(1L, 50, 50);
        Team awayLoser = team(2L, 50, 50);
        Team homeLoser = team(3L, 50, 50);
        Team awayWinner = team(4L, 50, 50);
        Team drawHome = team(5L, 50, 50);
        Team drawAway = team(6L, 50, 50);
        Match homeWin = match(homeWinner, awayLoser);
        Match awayWin = match(homeLoser, awayWinner);
        Match draw = match(drawHome, drawAway);
        givenWeekOneWithMatches(homeWin, awayWin, draw);
        when(scoreSimulator.simulate(anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(new SimulatedScore(3, 1), new SimulatedScore(0, 2), new SimulatedScore(2, 2));

        MatchWeekResponse response = matchSimulationService.playWeek(1);

        assertEquals(3, homeWin.getHomeScore());
        assertEquals(1, homeWin.getAwayScore());
        assertTrue(response.matches().stream().allMatch(MatchResponse::played));
        verify(matchRepository).saveAll(List.of(homeWin, awayWin, draw));

        assertEquals(60, homeWinner.getMorale(), "Galibiyet +10");
        assertEquals(40, awayLoser.getMorale(), "Maglubiyet -10");
        assertEquals(40, homeLoser.getMorale(), "Evinde kaybeden -10");
        assertEquals(60, awayWinner.getMorale(), "Deplasmanda kazanan +10");
        assertEquals(50, drawHome.getMorale(), "Beraberlikte moral degismez");
        assertEquals(50, drawAway.getMorale(), "Beraberlikte moral degismez");
    }

    @Test
    void moral0Ile100ArasindaKalir() {
        Team highMorale = team(1L, 50, 100);
        Team lowMorale = team(2L, 50, 0);
        givenWeekOneWithMatches(match(highMorale, lowMorale));
        when(scoreSimulator.simulate(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(new SimulatedScore(2, 0));

        matchSimulationService.playWeek(1);

        assertEquals(100, highMorale.getMorale());
        assertEquals(0, lowMorale.getMorale());
    }

    private void givenWeekOneWithMatches(Match... matches) {
        MatchWeek week = MatchWeek.builder().id(WEEK_ID).weekNumber(1).build();
        when(matchWeekRepository.findByWeekNumber(1)).thenReturn(Optional.of(week));
        when(matchRepository.findByMatchWeekIdOrderById(WEEK_ID)).thenReturn(List.of(matches));
    }

    private Match match(Team home, Team away) {
        return Match.builder().homeTeam(home).awayTeam(away).build();
    }

    private Team team(Long id, int strength, int morale) {
        return Team.builder().id(id).name("Takim " + id).strength(strength).morale(morale).build();
    }
}
