package com.footballleague.dto;

import com.footballleague.entity.Position;

/** Bir sezonun lig maçlarındaki oyuncu istatistikleri (puan durumu "Oyuncular" sekmesi). */
public record PlayerStatsResponse(
        Long playerId,
        String playerName,
        Position position,
        Long teamId,
        String teamName,
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
