package com.footballleague.dto;

import java.util.List;

import com.footballleague.entity.Competition;
import com.footballleague.entity.CupRound;
import com.footballleague.entity.InjurySeverity;
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
        // Serbest oyuncuda null
        Long teamId,
        String teamName,
        boolean teamActive,
        // Son 5 maç reytingleri ve bunlardan hesaplanan form çarpanı (0.9-1.1)
        List<Double> recentRatings,
        double form,
        InjurySeverity injurySeverity,
        List<SeasonLine> seasons,
        List<GoalLine> goals,
        List<InjuryLine> injuries,
        List<TransferResponse> transfers
) {

    /** matches: sakatlığın sürdüğü maç sayısı (bu özellikten önceki sakatlıklarda null). */
    public record InjuryLine(Long matchId, int seasonNumber, Competition competition, int weekNumber,
            CupRound cupRound, int minute, Integer matches) {
    }

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
