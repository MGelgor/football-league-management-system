package com.footballleague.dto;

import com.footballleague.entity.InboxMessageType;

/** actionable: kabul / ret bekleyen teklif (transfer, iş, altyapı). */
public record InboxMessageResponse(
        Long id,
        InboxMessageType type,
        String title,
        String body,
        int seasonNumber,
        boolean read,
        boolean resolved,
        boolean actionable,
        Long playerId,
        String playerName,
        Long teamId,
        String teamName,
        Long amount
) {
}
