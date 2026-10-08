package com.footballleague.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.footballleague.dto.StandingResponse;
import com.footballleague.entity.Competition;
import com.footballleague.entity.Match;
import com.footballleague.entity.Season;
import com.footballleague.entity.Team;
import com.footballleague.exception.SeasonNotFoundException;
import com.footballleague.repository.MatchRepository;
import com.footballleague.repository.SeasonRepository;
import com.footballleague.repository.TeamRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StandingsService {

    static final int RELEGATION_COUNT = 3;
    static final int FORM_LENGTH = 5;
    static final String CHAMPIONS_LEAGUE = "CHAMPIONS_LEAGUE";
    static final String EUROPA_LEAGUE = "EUROPA_LEAGUE";
    static final String RELEGATION = "RELEGATION";
    static final String PROMOTION = "PROMOTION";
    // 2. Lig'in ilk 3'ü 1. Lig'e çıkar, son 3'ü lig sisteminden ayrılır
    static final int PROMOTION_COUNT = 3;

    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;
    private final SeasonRepository seasonRepository;
    private final StandingsCalculator standingsCalculator;

    public static int checkDivision(int division) {
        if (division != 1 && division != 2) {
            throw new IllegalArgumentException("Lig 1 ya da 2 olmalı");
        }
        return division;
    }

    /** 1. Lig puan durumu. */
    public List<StandingResponse> getStandings(Long seasonId) {
        return getStandings(seasonId, 1);
    }

    /**
     * seasonId null ise güncel sezon; hiç sezon yoksa o kademenin aktif takımları sıfır puanla listelenir.
     * division: 1 (1. Lig) ya da 2 (2. Lig). Yalnızca o ligin maçları.
     */
    public List<StandingResponse> getStandings(Long seasonId, int division) {
        Optional<Season> season = seasonId == null
                ? seasonRepository.findTopByOrderBySeasonNumberDesc()
                : Optional.of(seasonRepository.findById(seasonId).orElseThrow(() -> new SeasonNotFoundException(seasonId)));

        if (season.isEmpty()) {
            return standingsCalculator.calculate(toTeamInfos(teamRepository.findByActiveTrueAndDivision(division)),
                    List.of());
        }

        List<Match> matches = matchRepository.findBySeasonAndCompetition(season.get().getId(),
                Competition.ofDivision(division));
        Map<Long, Team> participants = new LinkedHashMap<>();
        for (Match match : matches) {
            participants.putIfAbsent(match.getHomeTeam().getId(), match.getHomeTeam());
            participants.putIfAbsent(match.getAwayTeam().getId(), match.getAwayTeam());
        }
        List<StandingsCalculator.TeamInfo> teams = toTeamInfos(participants.values().stream().toList());
        List<Match> played = matches.stream().filter(Match::isPlayed).toList();

        List<StandingResponse> current = standingsCalculator.calculate(teams, toResults(played));
        Map<Long, Integer> previousRank = previousRanks(teams, played);
        Map<Long, List<String>> form = form(played);
        Set<Long> bigFour = participants.values().stream().filter(Team::isBigFour).map(Team::getId)
                .collect(Collectors.toSet());
        List<Long> relegated = division == 1 ? relegationZone(current, bigFour) : bottom(current, RELEGATION_COUNT);

        return current.stream()
                .map(row -> row.with(
                        previousRank.containsKey(row.teamId()) ? previousRank.get(row.teamId()) - row.rank() : 0,
                        form.getOrDefault(row.teamId(), List.of()),
                        division == 1 ? zone(row.rank(), relegated.contains(row.teamId()), current.size())
                                : secondLeagueZone(row.rank(), relegated.contains(row.teamId()), current.size())))
                .toList();
    }

    /** 2. Lig'in son count takımı (en alttaki önce). */
    static List<Long> bottom(List<StandingResponse> standings, int count) {
        if (standings.size() <= (PROMOTION_COUNT + count)) {
            return List.of();
        }
        return standings.subList(standings.size() - count, standings.size()).reversed().stream()
                .map(StandingResponse::teamId).toList();
    }

    private static String secondLeagueZone(int rank, boolean droppedOut, int teamCount) {
        if (droppedOut) {
            return RELEGATION;
        }
        return teamCount > PROMOTION_COUNT * 2 && rank <= PROMOTION_COUNT ? PROMOTION : null;
    }

    /** Düşecek takımlar: sondan başlayarak 4 büyükler hariç 3 takım (en alttaki önce). Küçük liglerde düşme yok. */
    static List<Long> relegationZone(List<StandingResponse> standings, Set<Long> bigFourTeamIds) {
        if (standings.size() <= RELEGATION_COUNT * 2) {
            return List.of();
        }
        List<Long> relegated = new ArrayList<>();
        for (int i = standings.size() - 1; i >= 0 && relegated.size() < RELEGATION_COUNT; i--) {
            Long teamId = standings.get(i).teamId();
            if (!bigFourTeamIds.contains(teamId)) {
                relegated.add(teamId);
            }
        }
        return relegated;
    }

    private static String zone(int rank, boolean relegated, int teamCount) {
        if (relegated) {
            return RELEGATION;
        }
        if (teamCount <= RELEGATION_COUNT * 2) {
            return null;
        }
        if (rank == 1) {
            return CHAMPIONS_LEAGUE;
        }
        return rank <= 3 ? EUROPA_LEAGUE : null;
    }

    /** Son oynanan haftadan önceki sıralama; ilk haftada (karşılaştırılacak sıra yokken) boş. */
    private Map<Long, Integer> previousRanks(List<StandingsCalculator.TeamInfo> teams, List<Match> played) {
        int lastPlayedWeek = played.stream().mapToInt(match -> match.getMatchWeek().getWeekNumber()).max().orElse(0);
        if (lastPlayedWeek <= 1) {
            return Map.of();
        }
        List<Match> beforeLastWeek = played.stream()
                .filter(match -> match.getMatchWeek().getWeekNumber() < lastPlayedWeek)
                .toList();
        return standingsCalculator.calculate(teams, toResults(beforeLastWeek)).stream()
                .collect(Collectors.toMap(StandingResponse::teamId, StandingResponse::rank));
    }

    /** Maçlar hafta sırasıyla geldiği için her takımın son 5 sonucu, eskiden yeniye. */
    private static Map<Long, List<String>> form(List<Match> played) {
        Map<Long, List<String>> results = new HashMap<>();
        for (Match match : played) {
            results.computeIfAbsent(match.getHomeTeam().getId(), id -> new ArrayList<>())
                    .add(result(match.getHomeScore(), match.getAwayScore()));
            results.computeIfAbsent(match.getAwayTeam().getId(), id -> new ArrayList<>())
                    .add(result(match.getAwayScore(), match.getHomeScore()));
        }
        results.replaceAll((id, list) -> List.copyOf(list.subList(Math.max(0, list.size() - FORM_LENGTH), list.size())));
        return results;
    }

    private static String result(int goalsFor, int goalsAgainst) {
        if (goalsFor > goalsAgainst) {
            return "G";
        }
        return goalsFor == goalsAgainst ? "B" : "M";
    }

    private static List<StandingsCalculator.TeamInfo> toTeamInfos(List<Team> teams) {
        return teams.stream().map(team -> new StandingsCalculator.TeamInfo(team.getId(), team.getName())).toList();
    }

    static List<StandingsCalculator.MatchResult> toResults(List<Match> matches) {
        return matches.stream()
                .map(match -> new StandingsCalculator.MatchResult(
                        match.getHomeTeam().getId(), match.getHomeScore(),
                        match.getAwayTeam().getId(), match.getAwayScore()))
                .toList();
    }
}
