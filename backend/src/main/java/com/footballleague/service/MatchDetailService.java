package com.footballleague.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.footballleague.dto.MatchDetailResponse;
import com.footballleague.dto.MatchResponse;
import com.footballleague.entity.Match;
import com.footballleague.entity.MatchAppearance;
import com.footballleague.entity.MatchEvent;
import com.footballleague.entity.MatchEventType;
import com.footballleague.entity.MatchTeamStats;
import com.footballleague.entity.Player;
import com.footballleague.entity.Team;
import com.footballleague.exception.MatchNotFoundException;
import com.footballleague.repository.MatchAppearanceRepository;
import com.footballleague.repository.MatchEventRepository;
import com.footballleague.repository.MatchRepository;
import com.footballleague.repository.MatchTeamStatsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchDetailService {

    private final MatchRepository matchRepository;
    private final MatchEventRepository matchEventRepository;
    private final MatchTeamStatsRepository matchTeamStatsRepository;
    private final MatchAppearanceRepository matchAppearanceRepository;
    private final MatchMapper matchMapper;

    public MatchDetailResponse getMatchDetail(Long id) {
        Match match = matchRepository.findDetailById(id).orElseThrow(() -> new MatchNotFoundException(id));
        MatchResponse summary = matchMapper.toMatchResponse(match);
        List<MatchEvent> events = matchEventRepository.findByMatchIdWithPlayers(id);
        List<MatchTeamStats> stats = matchTeamStatsRepository.findByMatchId(id);
        List<MatchAppearance> appearances = matchAppearanceRepository.findByMatchIdWithPlayers(id);

        return new MatchDetailResponse(
                match.getId(),
                match.getMatchWeek().getSeason().getSeasonNumber(),
                match.getMatchWeek().getWeekNumber(),
                match.getMatchWeek().getCompetition(),
                match.getMatchWeek().getCupRound(),
                match.isPlayed(),
                side(match.getHomeTeam(), match.getHomeScore(), match.getHomePenalties(), stats, events, appearances,
                        true),
                side(match.getAwayTeam(), match.getAwayScore(), match.getAwayPenalties(), stats, events, appearances,
                        false),
                summary.homeWinProbability(),
                summary.drawProbability(),
                summary.awayWinProbability(),
                events.stream().map(event -> toEvent(event, match)).toList());
    }

    private static MatchDetailResponse.Side side(Team team, Integer score, Integer penalties,
            List<MatchTeamStats> allStats, List<MatchEvent> events, List<MatchAppearance> appearances, boolean home) {
        MatchDetailResponse.Stats stats = allStats.stream()
                .filter(teamStats -> teamStats.isHome() == home)
                .findFirst()
                .map(teamStats -> new MatchDetailResponse.Stats(
                        teamStats.getPossession(),
                        teamStats.getShots(),
                        teamStats.getShotsOnTarget(),
                        teamStats.getCorners(),
                        teamStats.getFouls(),
                        teamStats.getOffsides(),
                        teamStats.getSaves(),
                        countCards(events, team, MatchEventType.YELLOW_CARD),
                        countCards(events, team, MatchEventType.RED_CARD)))
                .orElse(null);
        List<MatchDetailResponse.LineupEntry> lineup = appearances.stream()
                .filter(appearance -> appearance.getTeam().getId().equals(team.getId()))
                .map(MatchDetailService::toLineupEntry)
                .toList();
        return new MatchDetailResponse.Side(team.getId(), team.getName(), TeamService.logoUrl(team), score, penalties,
                stats, lineup);
    }

    private static MatchDetailResponse.LineupEntry toLineupEntry(MatchAppearance appearance) {
        Player player = appearance.getPlayer();
        return new MatchDetailResponse.LineupEntry(player.getId(), player.getName(), player.getPosition(),
                player.getShirtNumber(), appearance.isStarter(), appearance.getMinuteOn(), appearance.getMinuteOff(),
                appearance.getReplacedPlayer() != null ? appearance.getReplacedPlayer().getName() : null,
                appearance.getRating(), appearance.isPlayerOfTheMatch());
    }

    private static int countCards(List<MatchEvent> events, Team team, MatchEventType type) {
        return (int) events.stream()
                .filter(event -> event.getType() == type && event.getTeam().getId().equals(team.getId()))
                .count();
    }

    private static MatchDetailResponse.Event toEvent(MatchEvent event, Match match) {
        return new MatchDetailResponse.Event(
                event.getMinute(),
                event.getType(),
                event.getTeam().getId().equals(match.getHomeTeam().getId()),
                event.getPlayer().getId(),
                event.getPlayer().getName(),
                event.getPlayer().getPosition(),
                event.getPlayer().getShirtNumber(),
                event.getAssistPlayer() != null ? event.getAssistPlayer().getName() : null);
    }
}
