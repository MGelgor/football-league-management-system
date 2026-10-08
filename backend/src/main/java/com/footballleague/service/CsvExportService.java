package com.footballleague.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.footballleague.dto.MatchResponse;
import com.footballleague.dto.MatchWeekResponse;
import com.footballleague.dto.PlayerStatsResponse;
import com.footballleague.dto.StandingResponse;
import com.footballleague.entity.Position;

import lombok.RequiredArgsConstructor;

/**
 * Puan durumu, fikstür ve oyuncu istatistiklerini CSV'ye çevirir. Türkçe Excel'in doğrudan açabilmesi için
 * ayraç noktalı virgül, dosyanın başında UTF-8 BOM var.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CsvExportService {

    private static final String BOM = "﻿";
    private static final String SEPARATOR = ";";
    private static final Map<Position, String> POSITIONS = Map.of(
            Position.GOALKEEPER, "Kaleci",
            Position.DEFENDER, "Defans",
            Position.MIDFIELDER, "Orta saha",
            Position.FORWARD, "Forvet");

    private final StandingsService standingsService;
    private final FixtureService fixtureService;
    private final PlayerService playerService;

    public String standings(Long seasonId) {
        List<StandingResponse> rows = standingsService.getStandings(seasonId);
        return csv(List.of("Sıra", "Takım", "O", "G", "B", "M", "A", "Y", "AV", "P"),
                rows.stream().map(row -> List.<Object>of(row.rank(), row.teamName(), row.played(), row.won(),
                        row.drawn(), row.lost(), row.goalsFor(), row.goalsAgainst(), row.goalDifference(),
                        row.points())));
    }

    public String fixture(Long seasonId) {
        List<MatchWeekResponse> weeks = fixtureService.getFixture(seasonId);
        return csv(List.of("Hafta", "Ev sahibi", "Deplasman", "Ev gol", "Dep gol"),
                weeks.stream().flatMap(week -> week.matches().stream().map(match -> fixtureRow(week, match))));
    }

    public String players(Long seasonId) {
        List<PlayerStatsResponse> rows = playerService.getSeasonStats(seasonId);
        return csv(List.of("Oyuncu", "Mevki", "Takım", "Maç", "Dakika", "Gol", "Asist", "Sarı", "Kırmızı", "Reyting"),
                rows.stream().map(row -> List.<Object>of(row.playerName(), POSITIONS.get(row.position()), row.teamName(),
                        row.appearances(), row.minutes(), row.goals(), row.assists(), row.yellowCards(),
                        row.redCards(), row.averageRating() == null ? "" : row.averageRating())));
    }

    private static List<Object> fixtureRow(MatchWeekResponse week, MatchResponse match) {
        return List.of(week.weekNumber(), match.homeTeamName(), match.awayTeamName(),
                match.homeScore() == null ? "" : match.homeScore(),
                match.awayScore() == null ? "" : match.awayScore());
    }

    private static String csv(List<String> header, Stream<List<Object>> rows) {
        return BOM + Stream.concat(Stream.of(header.stream().map(Object.class::cast).toList()), rows)
                .map(row -> row.stream().map(CsvExportService::cell).collect(Collectors.joining(SEPARATOR)))
                .collect(Collectors.joining("\r\n", "", "\r\n"));
    }

    /** Ayraç, tırnak ya da satır sonu içeren hücre tırnak içine alınır. */
    static String cell(Object value) {
        String text = String.valueOf(value);
        if (text.contains(SEPARATOR) || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
