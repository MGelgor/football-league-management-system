package com.footballleague.dto;

import java.util.List;

import com.footballleague.entity.CupRound;

/**
 * status: NO_SEASON, LEAGUE_IN_PROGRESS (kupa lig bitince başlar), NOT_STARTED, IN_PROGRESS, FINISHED.
 */
public record CupResponse(
        Long seasonId,
        Integer seasonNumber,
        String status,
        List<Round> rounds,
        Long winnerTeamId,
        String winnerName
) {

    public record Round(CupRound round, boolean played, List<Tie> ties) {
    }

    /** homeSeed / awaySeed: takımın o sezonki lig sırası. winnerTeamId oynanmamışsa null. */
    public record Tie(MatchResponse match, int homeSeed, int awaySeed, Long winnerTeamId) {
    }
}
