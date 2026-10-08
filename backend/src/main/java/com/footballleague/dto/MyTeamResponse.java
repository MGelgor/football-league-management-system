package com.footballleague.dto;

import java.util.List;

import com.footballleague.entity.Competition;
import com.footballleague.entity.CupRound;

/**
 * "Takımım" panosu. active false ise mod kapalı ya da menajer işsiz (unemployed). nextMatch null: sırada maç yok
 * (sezon bitti, kupa bekliyor vb.); status bunu açıklar.
 */
public record MyTeamResponse(
        boolean active,
        String managerName,
        TeamResponse team,
        Integer seasonNumber,
        String status,
        NextMatch nextMatch,
        StandingResponse standing,
        List<StandingResponse> standingsAround,
        List<RecentResult> recentResults,
        List<PlayerResponse> unavailable,
        boolean transferWindowOpen,
        // Yönetim kurulunun güveni (0-100) ve sezon hedefleri
        int confidence,
        Integer targetRank,
        CupRound cupTarget,
        long unreadMessages,
        // Menajer kovuldu / istifa etti: gelen kutusunda iş teklifleri olabilir
        boolean unemployed
) {

    public record NextMatch(MatchResponse match, Competition competition, int weekNumber, CupRound cupRound,
            boolean home, boolean lineupSaved) {
    }

    /** result: G / B / M (kullanıcının takımına göre). */
    public record RecentResult(MatchResponse match, Competition competition, int weekNumber, CupRound cupRound,
            String result) {
    }
}
