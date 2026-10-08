package com.footballleague.entity;

/**
 * GOAL: takımın golü (penaltı golü de; MatchEvent.penalty). OWN_GOAL: oyuncu kendi kalesine attı, gol rakibe
 * yazılır (olayın takımı oyuncunun takımıdır). PENALTY_MISSED: kaçan penaltı. VAR_DISALLOWED: VAR'ın iptal ettiği
 * gol, skoru değiştirmez.
 */
public enum MatchEventType {
    GOAL,
    YELLOW_CARD,
    RED_CARD,
    INJURY,
    OWN_GOAL,
    PENALTY_MISSED,
    VAR_DISALLOWED
}
