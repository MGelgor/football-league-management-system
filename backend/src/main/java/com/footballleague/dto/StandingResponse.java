package com.footballleague.dto;

import java.util.List;

/**
 * rankChange: son oynanan haftadan önceki sıraya göre değişim (pozitif = yükseldi).
 * form: son 5 lig maçı, eskiden yeniye ("G", "B", "M").
 * zone: CHAMPIONS_LEAGUE (1.), EUROPA_LEAGUE (2-3.), RELEGATION (düşen 3 takım) ya da null.
 */
public record StandingResponse(
        int rank,
        Long teamId,
        String teamName,
        int played,
        int won,
        int drawn,
        int lost,
        int goalsFor,
        int goalsAgainst,
        int goalDifference,
        int points,
        int rankChange,
        List<String> form,
        String zone
) {

    public StandingResponse(int rank, Long teamId, String teamName, int played, int won, int drawn, int lost,
            int goalsFor, int goalsAgainst, int goalDifference, int points, int rankChange) {
        this(rank, teamId, teamName, played, won, drawn, lost, goalsFor, goalsAgainst, goalDifference, points,
                rankChange, List.of(), null);
    }

    public StandingResponse with(int newRankChange, List<String> newForm, String newZone) {
        return new StandingResponse(rank, teamId, teamName, played, won, drawn, lost, goalsFor, goalsAgainst,
                goalDifference, points, newRankChange, newForm, newZone);
    }
}
