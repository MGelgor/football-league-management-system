package com.footballleague.service;

import org.springframework.stereotype.Component;

import com.footballleague.dto.MatchResponse;
import com.footballleague.entity.Match;
import com.footballleague.entity.Team;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class MatchMapper {

    private final ScoreSimulator scoreSimulator;
    private final TacticsService tacticsService;

    MatchResponse toMatchResponse(Match match) {
        Team home = match.getHomeTeam();
        Team away = match.getAwayTeam();
        Integer homeWin = match.getHomeWinProbability();
        Integer draw = match.getDrawProbability();
        Integer awayWin = match.getAwayWinProbability();

        if (!match.isPlayed()) {
            ScoreSimulator.Probabilities probabilities = scoreSimulator.probabilities(scoreSimulator.expectedGoals(
                    tacticsService.setup(home, away, true), tacticsService.setup(away, home, false)));
            homeWin = probabilities.homeWinPercent();
            draw = probabilities.drawPercent();
            awayWin = probabilities.awayWinPercent();
        }

        return new MatchResponse(
                match.getId(),
                home.getId(),
                home.getName(),
                away.getId(),
                away.getName(),
                match.getHomeScore(),
                match.getAwayScore(),
                match.isPlayed(),
                homeWin,
                draw,
                awayWin,
                match.getHomePenalties(),
                match.getAwayPenalties());
    }
}
