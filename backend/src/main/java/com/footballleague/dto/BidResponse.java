package com.footballleague.dto;

import com.footballleague.entity.BidStatus;

/** Menajerin kapalı teklifi; currentTeamName oyuncunun şu anki takımı (serbestse null). */
public record BidResponse(Long id, Long playerId, String playerName, String currentTeamName, long fee,
        BidStatus status, String note) {
}
