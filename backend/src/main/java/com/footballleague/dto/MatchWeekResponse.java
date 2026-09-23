package com.footballleague.dto;

import java.util.List;

public record MatchWeekResponse(
        Integer weekNumber,
        List<MatchResponse> matches
) {
}
