package com.footballleague.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.footballleague.dto.HeadToHeadResponse;
import com.footballleague.dto.StandingResponse;
import com.footballleague.dto.TeamSeasonStatsResponse;
import com.footballleague.entity.Match;
import com.footballleague.entity.MatchEvent;
import com.footballleague.entity.MatchTeamStats;
import com.footballleague.entity.MatchWeek;
import com.footballleague.entity.Player;
import com.footballleague.entity.Season;
import com.footballleague.entity.Team;
import com.footballleague.exception.SeasonNotFoundException;
import com.footballleague.exception.TeamNotFoundException;
import com.footballleague.repository.MatchAppearanceRepository;
import com.footballleague.repository.MatchEventRepository;
import com.footballleague.repository.MatchRepository;
import com.footballleague.repository.MatchTeamStatsRepository;
import com.footballleague.repository.SeasonRepository;
import com.footballleague.repository.TeamRepository;

import lombok.RequiredArgsConstructor;

/** Takım sayfasındaki sezon istatistikleri / grafikler ve iki takımın karşılaştırması. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamStatsService {

    private final TeamRepository teamRepository;
    private final SeasonRepository seasonRepository;
    private final MatchRepository matchRepository;
    private final MatchTeamStatsRepository matchTeamStatsRepository;
    private final MatchEventRepository matchEventRepository;
    private final MatchAppearanceRepository matchAppearanceRepository;
    private final StandingsService standingsService;
    private final StandingsCalculator standingsCalculator;
    private final TeamService teamService;

    /** seasonId null ise güncel sezon. */
    public TeamSeasonStatsResponse getSeasonStats(Long teamId, Long seasonId) {
        if (!teamRepository.existsById(teamId)) {
            throw new TeamNotFoundException(teamId);
        }
        Optional<Season> season = seasonId == null
                ? seasonRepository.findTopByOrderBySeasonNumberDesc()
                : Optional.of(seasonRepository.findById(seasonId).orElseThrow(() -> new SeasonNotFoundException(seasonId)));
        Split empty = new Split();
        if (season.isEmpty()) {
            return new TeamSeasonStatsResponse(null, null, null, empty.toDto(), empty.toDto(), empty.toDto(), null,
                    0, 0, null, 0, 0, 0, null, null, List.of(), List.of());
        }
        Long id = season.get().getId();

        List<Match> leagueMatches = matchRepository.findLeagueMatchesBySeason(id);
        List<Match> played = leagueMatches.stream().filter(Match::isPlayed).toList();
        List<Match> teamMatches = played.stream()
                .filter(match -> match.getHomeTeam().getId().equals(teamId) || match.getAwayTeam().getId().equals(teamId))
                .toList();

        Split overall = new Split();
        Split home = new Split();
        Split away = new Split();
        int cleanSheets = 0;
        for (Match match : teamMatches) {
            boolean isHome = match.getHomeTeam().getId().equals(teamId);
            int goalsFor = isHome ? match.getHomeScore() : match.getAwayScore();
            int goalsAgainst = isHome ? match.getAwayScore() : match.getHomeScore();
            overall.add(goalsFor, goalsAgainst);
            (isHome ? home : away).add(goalsFor, goalsAgainst);
            cleanSheets += goalsAgainst == 0 ? 1 : 0;
        }

        List<MatchTeamStats> stats = matchTeamStatsRepository.findLeagueByTeamAndSeason(teamId, id);
        int shots = stats.stream().mapToInt(MatchTeamStats::getShots).sum();
        int shotsOnTarget = stats.stream().mapToInt(MatchTeamStats::getShotsOnTarget).sum();
        Double averagePossession = stats.isEmpty() ? null
                : Math.round(stats.stream().mapToInt(MatchTeamStats::getPossession).average().orElse(0) * 10) / 10.0;

        List<MatchEvent> events = matchEventRepository.findLeagueEventsByTeamAndSeason(teamId, id);
        Map<Long, PlayerTotals> totals = PlayerTotals.of(events,
                matchAppearanceRepository.findLeagueByTeamAndSeason(teamId, id));
        Map<Long, Player> players = new LinkedHashMap<>();
        events.forEach(event -> {
            players.putIfAbsent(event.getPlayer().getId(), event.getPlayer());
            if (event.getAssistPlayer() != null) {
                players.putIfAbsent(event.getAssistPlayer().getId(), event.getAssistPlayer());
            }
        });

        Optional<StandingResponse> standing = standingsService.getStandings(id).stream()
                .filter(row -> row.teamId().equals(teamId))
                .findFirst();

        return new TeamSeasonStatsResponse(
                id,
                season.get().getSeasonNumber(),
                standing.map(StandingResponse::rank).orElse(null),
                overall.toDto(), home.toDto(), away.toDto(),
                averagePossession,
                shots,
                shotsOnTarget,
                shots == 0 ? null : (int) Math.round(shotsOnTarget * 100.0 / shots),
                cleanSheets,
                totals.values().stream().mapToInt(t -> t.yellowCards).sum(),
                totals.values().stream().mapToInt(t -> t.redCards).sum(),
                best(players, totals, true),
                best(players, totals, false),
                standing.map(StandingResponse::form).orElse(List.of()),
                weeklyHistory(teamId, leagueMatches, played, stats));
    }

    /** İki takım arasındaki tüm oynanmış maçlar ve özet. */
    public HeadToHeadResponse headToHead(Long teamA, Long teamB) {
        if (teamA.equals(teamB)) {
            throw new IllegalArgumentException("Karşılaştırma için iki farklı takım seçin");
        }
        List<Match> matches = matchRepository.findHeadToHead(teamA, teamB);
        int winsA = 0;
        int winsB = 0;
        int draws = 0;
        int goalsA = 0;
        int goalsB = 0;
        List<HeadToHeadResponse.MatchLine> lines = new ArrayList<>();
        for (Match match : matches) {
            boolean aHome = match.getHomeTeam().getId().equals(teamA);
            int forA = aHome ? match.getHomeScore() : match.getAwayScore();
            int forB = aHome ? match.getAwayScore() : match.getHomeScore();
            goalsA += forA;
            goalsB += forB;
            if (forA > forB) {
                winsA++;
            } else if (forB > forA) {
                winsB++;
            } else {
                draws++;
            }
            MatchWeek week = match.getMatchWeek();
            lines.add(new HeadToHeadResponse.MatchLine(match.getId(), week.getSeason().getSeasonNumber(),
                    week.getCompetition(), week.getWeekNumber(), week.getCupRound(), match.getHomeTeam().getId(),
                    match.getHomeTeam().getName(), match.getAwayTeam().getName(), match.getHomeScore(),
                    match.getAwayScore(), match.getHomePenalties(), match.getAwayPenalties()));
        }
        return new HeadToHeadResponse(teamService.getTeam(teamA), teamService.getTeam(teamB), matches.size(), winsA,
                draws, winsB, goalsA, goalsB, lines);
    }

    /** Her oynanmış haftadan sonra takımın sırası, puanı ve gücü. */
    private List<TeamSeasonStatsResponse.WeekPoint> weeklyHistory(Long teamId, List<Match> leagueMatches,
            List<Match> played, List<MatchTeamStats> stats) {
        if (played.stream().noneMatch(match -> match.getHomeTeam().getId().equals(teamId)
                || match.getAwayTeam().getId().equals(teamId))) {
            return List.of();
        }
        Map<Long, String> names = new LinkedHashMap<>();
        leagueMatches.forEach(match -> {
            names.putIfAbsent(match.getHomeTeam().getId(), match.getHomeTeam().getName());
            names.putIfAbsent(match.getAwayTeam().getId(), match.getAwayTeam().getName());
        });
        List<StandingsCalculator.TeamInfo> teams = names.entrySet().stream()
                .map(entry -> new StandingsCalculator.TeamInfo(entry.getKey(), entry.getValue()))
                .toList();
        Map<Integer, Integer> strengthByWeek = stats.stream()
                .filter(teamStats -> teamStats.getStrengthAfter() != null)
                .collect(Collectors.toMap(teamStats -> teamStats.getMatch().getMatchWeek().getWeekNumber(),
                        MatchTeamStats::getStrengthAfter, (a, b) -> b));

        int lastWeek = played.stream().mapToInt(match -> match.getMatchWeek().getWeekNumber()).max().orElse(0);
        List<TeamSeasonStatsResponse.WeekPoint> points = new ArrayList<>();
        for (int week = 1; week <= lastWeek; week++) {
            int upToWeek = week;
            List<Match> sofar = played.stream().filter(match -> match.getMatchWeek().getWeekNumber() <= upToWeek)
                    .toList();
            standingsCalculator.calculate(teams, StandingsService.toResults(sofar)).stream()
                    .filter(row -> row.teamId().equals(teamId))
                    .findFirst()
                    .ifPresent(row -> points.add(new TeamSeasonStatsResponse.WeekPoint(upToWeek, row.rank(),
                            row.points(), strengthByWeek.get(upToWeek))));
        }
        return points;
    }

    private static TeamSeasonStatsResponse.PlayerRef best(Map<Long, Player> players, Map<Long, PlayerTotals> totals,
            boolean goals) {
        return players.values().stream()
                .map(player -> new TeamSeasonStatsResponse.PlayerRef(player.getId(), player.getName(),
                        goals ? PlayerTotals.get(totals, player.getId()).goals
                                : PlayerTotals.get(totals, player.getId()).assists))
                .filter(ref -> ref.value() > 0)
                .max(Comparator.comparingInt(TeamSeasonStatsResponse.PlayerRef::value))
                .orElse(null);
    }

    private static final class Split {
        private int played;
        private int won;
        private int drawn;
        private int lost;
        private int goalsFor;
        private int goalsAgainst;

        private void add(int scored, int conceded) {
            played++;
            goalsFor += scored;
            goalsAgainst += conceded;
            if (scored > conceded) {
                won++;
            } else if (scored == conceded) {
                drawn++;
            } else {
                lost++;
            }
        }

        private TeamSeasonStatsResponse.Split toDto() {
            return new TeamSeasonStatsResponse.Split(played, won, drawn, lost, goalsFor, goalsAgainst, won * 3 + drawn);
        }
    }
}
