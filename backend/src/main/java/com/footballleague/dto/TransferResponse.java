package com.footballleague.dto;

import com.footballleague.entity.Position;

/** fromTeamId null = serbest oyuncu imzası. */
public record TransferResponse(
        Long id,
        Long playerId,
        String playerName,
        Position position,
        Long fromTeamId,
        String fromTeamName,
        Long toTeamId,
        String toTeamName,
        long fee,
        int seasonNumber
) {
}
