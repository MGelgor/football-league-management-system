package com.footballleague.dto;

import java.util.List;

/** Oynanmış bir haftanın (ya da kupa turunun) canlı yayın için dakika sırasına dizilmiş olayları. */
public record LiveTimeline(int weekNumber, List<LiveMatch> matches, List<LiveItem> items) {

    public record LiveMatch(Long matchId, Long homeTeamId, String homeTeamName, Long awayTeamId, String awayTeamName,
            int homeScore, int awayScore, Integer homePenalties, Integer awayPenalties) {
    }

    /**
     * type: MatchEventType adı ya da SUBSTITUTION; home: oyuncunun takımı ev sahibi mi (kendi kalesine golde gol
     * rakibe yazılır); text: yorum cümlesi.
     */
    public record LiveItem(Long matchId, int minute, String type, boolean home, Long playerId, String playerName,
            String assistName, String text, boolean penalty) {
    }

    /** SSE "minute" olayı: o dakikanın olayları ve maçların o ana kadarki skorları. */
    public record LiveMinute(int minute, String phase, List<LiveItem> items, List<LiveScore> scores) {
    }

    public record LiveScore(Long matchId, int home, int away) {
    }
}
