package com.footballleague.dto;

import java.util.List;

import com.footballleague.entity.Competition;
import com.footballleague.entity.CupRound;

/** İki takım arasındaki tüm oynanmış maçlar (lig + kupa). Galibiyet / beraberlik 90 dakikalık skora göre. */
public record HeadToHeadResponse(
        TeamResponse teamA,
        TeamResponse teamB,
        int played,
        int winsA,
        int draws,
        int winsB,
        int goalsA,
        int goalsB,
        List<MatchLine> matches
) {

    public record MatchLine(
            Long matchId,
            int seasonNumber,
            Competition competition,
            int weekNumber,
            CupRound cupRound,
            Long homeTeamId,
            String homeTeamName,
            String awayTeamName,
            int homeScore,
            int awayScore,
            Integer homePenalties,
            Integer awayPenalties
    ) {
    }
}
