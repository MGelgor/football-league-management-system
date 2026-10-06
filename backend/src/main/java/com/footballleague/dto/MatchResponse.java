package com.footballleague.dto;

/** Olasılıklar yüzde cinsinden: oynanmamış maçta güncel güce göre, oynanmış maçta maç öncesi kaydedilen değer. */
public record MatchResponse(
        Long id,
        Long homeTeamId,
        String homeTeamName,
        Long awayTeamId,
        String awayTeamName,
        Integer homeScore,
        Integer awayScore,
        boolean played,
        Integer homeWinProbability,
        Integer drawProbability,
        Integer awayWinProbability,
        // Yalnızca beraberlikle biten kupa maçlarında
        Integer homePenalties,
        Integer awayPenalties
) {
}
