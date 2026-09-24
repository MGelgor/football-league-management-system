package com.footballleague.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.footballleague.dto.StandingResponse;
import com.footballleague.entity.Match;
import com.footballleague.repository.MatchRepository;
import com.footballleague.repository.TeamRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StandingsService {

    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;
    private final StandingsCalculator standingsCalculator;

    public List<StandingResponse> getStandings() {
        List<StandingsCalculator.TeamInfo> teams = teamRepository.findAll().stream()
                .map(team -> new StandingsCalculator.TeamInfo(team.getId(), team.getName()))
                .toList();

        List<StandingsCalculator.MatchResult> playedMatches = matchRepository.findAllWithTeamsOrderByWeek().stream()
                .filter(Match::isPlayed)
                .map(match -> new StandingsCalculator.MatchResult(
                        match.getHomeTeam().getId(), match.getHomeScore(),
                        match.getAwayTeam().getId(), match.getAwayScore()))
                .toList();

        return standingsCalculator.calculate(teams, playedMatches);
    }
}
