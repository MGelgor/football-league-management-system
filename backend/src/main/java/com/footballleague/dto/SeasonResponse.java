package com.footballleague.dto;

import java.util.List;

public record SeasonResponse(
        Long id,
        int seasonNumber,
        boolean finished,
        Long championTeamId,
        String championName,
        long totalMatches,
        long playedMatches,
        Long cupWinnerTeamId,
        String cupWinnerName,
        // Sezon sonunda ligden düşen / lige çıkan takımlar
        List<TeamRef> relegated,
        List<TeamRef> promoted
) {

    public record TeamRef(Long id, String name) {
    }
}
