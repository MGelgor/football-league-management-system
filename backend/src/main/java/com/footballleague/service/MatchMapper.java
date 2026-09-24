package com.footballleague.service;

import com.footballleague.dto.MatchResponse;
import com.footballleague.entity.Match;

final class MatchMapper {

    private MatchMapper() {
    }

    static MatchResponse toMatchResponse(Match match) {
        return new MatchResponse(
                match.getId(),
                match.getHomeTeam().getId(),
                match.getHomeTeam().getName(),
                match.getAwayTeam().getId(),
                match.getAwayTeam().getName(),
                match.getHomeScore(),
                match.getAwayScore(),
                match.isPlayed());
    }
}
