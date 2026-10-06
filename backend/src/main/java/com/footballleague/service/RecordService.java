package com.footballleague.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.footballleague.dto.RecordResponse;
import com.footballleague.dto.StandingResponse;
import com.footballleague.entity.Match;
import com.footballleague.entity.MatchEvent;
import com.footballleague.entity.Player;
import com.footballleague.entity.Season;
import com.footballleague.entity.Team;
import com.footballleague.repository.MatchEventRepository;
import com.footballleague.repository.MatchRepository;
import com.footballleague.repository.SeasonRepository;

import lombok.RequiredArgsConstructor;

/** Tüm sezonların lig maçlarından hesaplanan tarihsel rekorlar (kupa maçları hariç). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecordService {

    private final MatchRepository matchRepository;
    private final MatchEventRepository matchEventRepository;
    private final SeasonRepository seasonRepository;
    private final StandingsCalculator standingsCalculator;

    public List<RecordResponse> getRecords() {
        List<Match> matches = matchRepository.findAllPlayedLeagueMatches();
        List<RecordResponse> records = new ArrayList<>();
        if (matches.isEmpty()) {
            return records;
        }

        Match highestScoring = matches.stream()
                .max(Comparator.comparingInt((Match match) -> match.getHomeScore() + match.getAwayScore())).orElseThrow();
        records.add(matchRecord("En gollü maç", highestScoring,
                (highestScoring.getHomeScore() + highestScoring.getAwayScore()) + " gol"));

        Match biggestWin = matches.stream()
                .max(Comparator.comparingInt((Match match) -> Math.abs(match.getHomeScore() - match.getAwayScore())))
                .orElseThrow();
        records.add(matchRecord("En farklı galibiyet", biggestWin,
                Math.abs(biggestWin.getHomeScore() - biggestWin.getAwayScore()) + " fark"));

        Map<Integer, List<Match>> bySeason = matches.stream()
                .collect(Collectors.groupingBy(match -> match.getMatchWeek().getSeason().getSeasonNumber(),
                        LinkedHashMap::new, Collectors.toList()));
        records.add(streakRecord("En uzun galibiyet serisi", bySeason, true));
        records.add(streakRecord("En uzun yenilmezlik serisi", bySeason, false));
        records.addAll(seasonTableRecords(bySeason));
        records.addAll(scorerRecords(matchEventRepository.findAllLeagueGoals()));
        records.addAll(trophyRecords(seasonRepository.findAllByOrderBySeasonNumberDesc()));
        return records;
    }

    private static RecordResponse matchRecord(String title, Match match, String value) {
        return new RecordResponse(title,
                match.getHomeTeam().getName() + " " + match.getHomeScore() + " - " + match.getAwayScore() + " "
                        + match.getAwayTeam().getName(),
                value, seasonWeek(match), null, null, match.getId());
    }

    /** Takım başına, sezon içinde ardışık galibiyet (onlyWins) ya da yenilmezlik serisinin en uzunu. */
    private static RecordResponse streakRecord(String title, Map<Integer, List<Match>> bySeason, boolean onlyWins) {
        Team bestTeam = null;
        int bestLength = 0;
        int bestSeason = 0;
        for (Map.Entry<Integer, List<Match>> season : bySeason.entrySet()) {
            Map<Team, Integer> current = new HashMap<>();
            for (Match match : season.getValue()) {
                for (boolean home : new boolean[] {true, false}) {
                    Team team = home ? match.getHomeTeam() : match.getAwayTeam();
                    int goalsFor = home ? match.getHomeScore() : match.getAwayScore();
                    int goalsAgainst = home ? match.getAwayScore() : match.getHomeScore();
                    boolean continues = onlyWins ? goalsFor > goalsAgainst : goalsFor >= goalsAgainst;
                    int length = continues ? current.getOrDefault(team, 0) + 1 : 0;
                    current.put(team, length);
                    if (length > bestLength) {
                        bestLength = length;
                        bestTeam = team;
                        bestSeason = season.getKey();
                    }
                }
            }
        }
        if (bestTeam == null) {
            return new RecordResponse(title, "–", "0 maç", null, null, null, null);
        }
        return new RecordResponse(title, bestTeam.getName(), bestLength + " maç", "Sezon " + bestSeason,
                bestTeam.getId(), null, null);
    }

    /** Sezonda en çok puan ve en çok gol (takım). */
    private List<RecordResponse> seasonTableRecords(Map<Integer, List<Match>> bySeason) {
        record Row(int season, StandingResponse standing) {
        }
        List<Row> rows = new ArrayList<>();
        for (Map.Entry<Integer, List<Match>> season : bySeason.entrySet()) {
            Map<Long, String> names = new LinkedHashMap<>();
            season.getValue().forEach(match -> {
                names.putIfAbsent(match.getHomeTeam().getId(), match.getHomeTeam().getName());
                names.putIfAbsent(match.getAwayTeam().getId(), match.getAwayTeam().getName());
            });
            List<StandingsCalculator.TeamInfo> teams = names.entrySet().stream()
                    .map(entry -> new StandingsCalculator.TeamInfo(entry.getKey(), entry.getValue()))
                    .toList();
            standingsCalculator.calculate(teams, StandingsService.toResults(season.getValue()))
                    .forEach(standing -> rows.add(new Row(season.getKey(), standing)));
        }
        Row mostPoints = rows.stream().max(Comparator.comparingInt(row -> row.standing().points())).orElseThrow();
        Row mostGoals = rows.stream().max(Comparator.comparingInt(row -> row.standing().goalsFor())).orElseThrow();
        return List.of(
                new RecordResponse("Bir sezonda en çok puan", mostPoints.standing().teamName(),
                        mostPoints.standing().points() + " puan", "Sezon " + mostPoints.season(),
                        mostPoints.standing().teamId(), null, null),
                new RecordResponse("Bir sezonda en çok gol (takım)", mostGoals.standing().teamName(),
                        mostGoals.standing().goalsFor() + " gol", "Sezon " + mostGoals.season(),
                        mostGoals.standing().teamId(), null, null));
    }

    /** Bir sezonda en çok gol atan oyuncu ve tüm zamanların gol kralı. */
    private static List<RecordResponse> scorerRecords(List<MatchEvent> goals) {
        if (goals.isEmpty()) {
            return List.of();
        }
        record SeasonPlayer(int season, Long playerId) {
        }
        Map<Long, Player> players = goals.stream().map(MatchEvent::getPlayer)
                .collect(Collectors.toMap(Player::getId, Function.identity(), (a, b) -> a));
        Map<SeasonPlayer, Long> perSeason = goals.stream().collect(Collectors.groupingBy(
                goal -> new SeasonPlayer(goal.getMatch().getMatchWeek().getSeason().getSeasonNumber(),
                        goal.getPlayer().getId()),
                Collectors.counting()));
        Map<Long, Long> allTime = goals.stream()
                .collect(Collectors.groupingBy(goal -> goal.getPlayer().getId(), Collectors.counting()));

        Map.Entry<SeasonPlayer, Long> seasonBest = perSeason.entrySet().stream()
                .max(Map.Entry.comparingByValue()).orElseThrow();
        Map.Entry<Long, Long> allTimeBest = allTime.entrySet().stream()
                .max(Map.Entry.comparingByValue()).orElseThrow();
        Player seasonPlayer = players.get(seasonBest.getKey().playerId());
        Player allTimePlayer = players.get(allTimeBest.getKey());
        return List.of(
                new RecordResponse("Bir sezonda en çok gol (oyuncu)", seasonPlayer.getName(),
                        seasonBest.getValue() + " gol",
                        seasonPlayer.getTeam().getName() + " · Sezon " + seasonBest.getKey().season(),
                        null, seasonPlayer.getId(), null),
                new RecordResponse("Tüm zamanların gol kralı", allTimePlayer.getName(),
                        allTimeBest.getValue() + " gol", allTimePlayer.getTeam().getName(),
                        null, allTimePlayer.getId(), null));
    }

    private static List<RecordResponse> trophyRecords(List<Season> seasons) {
        List<RecordResponse> records = new ArrayList<>();
        trophyRecord("En çok şampiyonluk", seasons.stream().map(Season::getChampion).toList(), "şampiyonluk")
                .ifPresent(records::add);
        trophyRecord("En çok kupa", seasons.stream().map(Season::getCupWinner).toList(), "kupa")
                .ifPresent(records::add);
        return records;
    }

    private static Optional<RecordResponse> trophyRecord(String title, List<Team> winners, String unit) {
        Map<Team, Long> counts = winners.stream().filter(team -> team != null)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(best -> new RecordResponse(title, best.getKey().getName(), best.getValue() + " " + unit, null,
                        best.getKey().getId(), null, null));
    }

    private static String seasonWeek(Match match) {
        return "Sezon " + match.getMatchWeek().getSeason().getSeasonNumber() + " · Hafta "
                + match.getMatchWeek().getWeekNumber();
    }
}
