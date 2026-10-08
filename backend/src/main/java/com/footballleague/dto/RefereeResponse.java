package com.footballleague.dto;

/** penalties: verdiği penaltılar (atılan + kaçan); ortalamalar maç başına, hiç maçı yoksa 0. */
public record RefereeResponse(
        Long id,
        String name,
        int strictness,
        long matches,
        long yellowCards,
        long redCards,
        long penalties,
        double yellowCardsPerMatch,
        double redCardsPerMatch
) {
}
