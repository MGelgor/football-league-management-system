package com.footballleague.dto;

import java.util.List;

import com.footballleague.entity.Competition;
import com.footballleague.entity.CupRound;
import com.footballleague.entity.MatchEventType;
import com.footballleague.entity.Position;

public record MatchDetailResponse(
        Long id,
        int seasonNumber,
        int weekNumber,
        Competition competition,
        CupRound cupRound,
        boolean played,
        Side home,
        Side away,
        Integer homeWinProbability,
        Integer drawProbability,
        Integer awayWinProbability,
        List<Event> events
) {

    /** stats, maç oynanmadıysa null; penalties yalnızca beraberlikle biten kupa maçlarında. */
    public record Side(Long teamId, String teamName, String logoUrl, Integer score, Integer penalties, Stats stats,
            List<LineupEntry> lineup) {
    }

    public record Stats(
            int possession,
            int shots,
            int shotsOnTarget,
            int corners,
            int fouls,
            int offsides,
            int saves,
            int yellowCards,
            int redCards
    ) {
    }

    public record Event(
            int minute,
            MatchEventType type,
            boolean home,
            Long playerId,
            String playerName,
            Position position,
            Integer shirtNumber,
            String assistName
    ) {
    }

    /** İlk 11 ve oyuna girenler; minuteOn 0 = ilk 11, replacedPlayerName yalnızca oyuna girenlerde. */
    public record LineupEntry(
            Long playerId,
            String playerName,
            Position position,
            Integer shirtNumber,
            boolean starter,
            int minuteOn,
            int minuteOff,
            String replacedPlayerName,
            double rating,
            boolean playerOfTheMatch
    ) {
    }
}
