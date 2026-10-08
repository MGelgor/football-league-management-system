package com.footballleague.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.footballleague.entity.MatchAppearance;
import com.footballleague.entity.MatchEvent;

/** Olay ve maç kayıtlarından oyuncu başına toplamlar (istatistikler ayrı tabloda tutulmaz, buradan sayılır). */
final class PlayerTotals {

    int appearances;
    int minutes;
    int goals;
    int ownGoals;
    int assists;
    int yellowCards;
    int redCards;
    int playerOfTheMatch;
    private double ratingSum;

    Double averageRating() {
        return appearances == 0 ? null : Math.round(ratingSum / appearances * 100) / 100.0;
    }

    /** Oyuncu id → toplamlar. */
    static Map<Long, PlayerTotals> of(List<MatchEvent> events, List<MatchAppearance> appearances) {
        Map<Long, PlayerTotals> totals = new HashMap<>();
        for (MatchAppearance appearance : appearances) {
            PlayerTotals player = totals.computeIfAbsent(appearance.getPlayer().getId(), id -> new PlayerTotals());
            player.appearances++;
            player.minutes += appearance.minutesPlayed();
            player.ratingSum += appearance.getRating();
            player.playerOfTheMatch += appearance.isPlayerOfTheMatch() ? 1 : 0;
        }
        for (MatchEvent event : events) {
            PlayerTotals player = totals.computeIfAbsent(event.getPlayer().getId(), id -> new PlayerTotals());
            switch (event.getType()) {
                case GOAL -> player.goals++;
                case YELLOW_CARD -> player.yellowCards++;
                case RED_CARD -> player.redCards++;
                case OWN_GOAL -> player.ownGoals++;
                case INJURY, PENALTY_MISSED, VAR_DISALLOWED -> {
                }
            }
            if (event.getAssistPlayer() != null) {
                totals.computeIfAbsent(event.getAssistPlayer().getId(), id -> new PlayerTotals()).assists++;
            }
        }
        return totals;
    }

    static PlayerTotals get(Map<Long, PlayerTotals> totals, Long playerId) {
        return totals.getOrDefault(playerId, new PlayerTotals());
    }
}
