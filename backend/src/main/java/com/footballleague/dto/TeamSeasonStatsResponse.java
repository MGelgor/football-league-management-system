package com.footballleague.dto;

import java.util.List;

/**
 * Bir takımın bir sezondaki lig istatistikleri. seasonId null: takım henüz hiç sezon oynamadı.
 * weeks: her oynanan haftadan sonraki sıra, puan ve güç (grafikler için).
 */
public record TeamSeasonStatsResponse(
        Long seasonId,
        Integer seasonNumber,
        Integer rank,
        Split overall,
        Split home,
        Split away,
        Double averagePossession,
        int shots,
        int shotsOnTarget,
        Integer shotAccuracy,
        int cleanSheets,
        int yellowCards,
        int redCards,
        PlayerRef topScorer,
        PlayerRef topAssister,
        List<String> form,
        List<WeekPoint> weeks
) {

    public record Split(int played, int won, int drawn, int lost, int goalsFor, int goalsAgainst, int points) {
    }

    public record PlayerRef(Long playerId, String name, int value) {
    }

    /** strength: maçtan sonraki takım gücü (eski kayıtlarda null olabilir). */
    public record WeekPoint(int week, int rank, int points, Integer strength) {
    }
}
