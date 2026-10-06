package com.footballleague.dto;

import java.util.List;

import com.footballleague.entity.Competition;
import com.footballleague.entity.CupRound;
import com.footballleague.entity.Position;

/** Oyuncu sayfası: profil, sezon / turnuva bazında istatistikler ve gol / asist listesi. */
public record PlayerProfileResponse(
        Long id,
        String name,
        Position position,
        Integer shirtNumber,
        Integer strength,
        Integer age,
        int lastStrengthChange,
        boolean retired,
        int suspendedMatches,
        int injuredMatches,
        Long teamId,
        String teamName,
        boolean teamActive,
        List<SeasonLine> seasons,
        List<GoalLine> goals
) {

    public record SeasonLine(
            int seasonNumber,
            Competition competition,
            int appearances,
            int minutes,
            int goals,
            int assists,
            int yellowCards,
            int redCards,
            Double averageRating,
            int playerOfTheMatch
    ) {
    }

    /** type: GOAL (oyuncu attı) ya da ASSIST (oyuncu asist yaptı); partnerName asist yapan / golü atan. */
    public record GoalLine(
            Long matchId,
            int seasonNumber,
            Competition competition,
            int weekNumber,
            CupRound cupRound,
            String opponentName,
            boolean home,
            String score,
            int minute,
            String type,
            String partnerName
    ) {
    }
}
