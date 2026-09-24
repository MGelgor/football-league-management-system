package com.footballleague.dto;

import java.util.List;

public record SeasonResultResponse(
        String championName,
        int championPoints,
        List<StandingResponse> finalStandings
) {
}
