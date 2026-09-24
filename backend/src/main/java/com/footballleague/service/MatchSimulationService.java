package com.footballleague.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MatchSimulationService {

    private static final int MORALE_CHANGE = 10;
    private static final int MORALE_MIN = 0;
    private static final int MORALE_MAX = 100;

    private final MatchWeekRepository matchWeekRepository;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final ScoreSimulator scoreSimulator;

    public MatchWeekResponse playWeek(Integer weekNumber) {
        MatchWeek week = matchWeekRepository.findByWeekNumber(weekNumber)
                .orElseThrow(() -> new MatchWeekNotFoundException(weekNumber));

        List<Match> matches = matchRepository.findByMatchWeekIdOrderById(week.getId());
        if (matches.stream().anyMatch(Match::isPlayed)) {
            throw new WeekAlreadyPlayedException(weekNumber);
        }

        Set<Team> touchedTeams = new LinkedHashSet<>();
        for (Match match : matches) {
            simulateMatch(match);
            touchedTeams.add(match.getHomeTeam());
            touchedTeams.add(match.getAwayTeam());
        }

        matchRepository.saveAll(matches);
        teamRepository.saveAll(touchedTeams);

        List<MatchResponse> matchResponses = matches.stream().map(MatchMapper::toMatchResponse).toList();
        return new MatchWeekResponse(weekNumber, matchResponses);
    }

    private void simulateMatch(Match match) {
        Team home = match.getHomeTeam();
        Team away = match.getAwayTeam();

        ScoreSimulator.SimulatedScore score = scoreSimulator.simulate(
                home.getStrength(), home.getMorale(), away.getStrength(), away.getMorale());

        match.setHomeScore(score.homeGoals());
        match.setAwayScore(score.awayGoals());

        applyMoraleChange(home, away, score.homeGoals(), score.awayGoals());
    }

    private void applyMoraleChange(Team home, Team away, int homeGoals, int awayGoals) {
        if (homeGoals > awayGoals) {
            adjustMorale(home, MORALE_CHANGE);
            adjustMorale(away, -MORALE_CHANGE);
        } else if (homeGoals < awayGoals) {
            adjustMorale(away, MORALE_CHANGE);
            adjustMorale(home, -MORALE_CHANGE);
        }
    }

    private void adjustMorale(Team team, int delta) {
        team.setMorale(Math.min(MORALE_MAX, Math.max(MORALE_MIN, team.getMorale() + delta)));
    }
}
