package com.footballleague.dto;

import com.footballleague.entity.Position;

/** Transfer piyasasındaki oyuncu; askingPrice satan takımın kabul edeceği bedel (serbest oyuncuda 0). */
public record MarketPlayerResponse(
        Long playerId,
        String name,
        Position position,
        int age,
        int strength,
        double form,
        Long teamId,
        String teamName,
        boolean freeAgent,
        long marketValue,
        long askingPrice,
        long expectedWage,
        Integer contractUntil
) {
}
