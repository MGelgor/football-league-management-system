package com.footballleague.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.footballleague.dto.MatchWeekResponse;
import com.footballleague.entity.Competition;
import com.footballleague.entity.Match;
import com.footballleague.entity.MatchAppearance;
import com.footballleague.entity.MatchEvent;
import com.footballleague.entity.MatchTeamStats;
import com.footballleague.entity.MatchWeek;
import com.footballleague.entity.Player;
import com.footballleague.entity.Season;
import com.footballleague.entity.Team;
import com.footballleague.exception.FixtureNotGeneratedException;
import com.footballleague.exception.MatchWeekNotFoundException;
import com.footballleague.exception.SeasonFinishedException;
import com.footballleague.exception.WeekAlreadyPlayedException;
import com.footballleague.exception.WeekOrderException;
import com.footballleague.repository.MatchAppearanceRepository;
import com.footballleague.repository.MatchEventRepository;
import com.footballleague.repository.MatchRepository;
import com.footballleague.repository.MatchTeamStatsRepository;
import com.footballleague.repository.MatchWeekRepository;
import com.footballleague.repository.PlayerRepository;
import com.footballleague.repository.SeasonRepository;
import com.footballleague.repository.TeamRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MatchSimulationService {

    private static final int MORALE_CHANGE = 10;
    private static final int MORALE_MIN = 0;
    private static final int MORALE_MAX = 100;
    // Güç değişimi = (alınan puan - beklenen puan) * bu katsayı, yuvarlanmış (pratikte -2..+2)
    private static final double STRENGTH_CHANGE_FACTOR = 0.7;
    // Sezonda her bu kadar sarı kartta 1 maç ceza
    static final int YELLOW_CARDS_PER_SUSPENSION = 4;
    static final int MAX_INJURY_MATCHES = 3;

    private final SeasonRepository seasonRepository;
    private final MatchWeekRepository matchWeekRepository;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final MatchEventRepository matchEventRepository;
    private final MatchTeamStatsRepository matchTeamStatsRepository;
    private final MatchAppearanceRepository matchAppearanceRepository;
    private final ScoreSimulator scoreSimulator;
    private final MatchDetailGenerator matchDetailGenerator;
    private final MatchMapper matchMapper;
    private final SeasonEndService seasonEndService;

    public MatchWeekResponse playWeek(Integer weekNumber) {
        Season season = seasonRepository.findTopByOrderBySeasonNumberDesc()
                .orElseThrow(FixtureNotGeneratedException::new);
        MatchWeek week = matchWeekRepository.findBySeasonIdAndWeekNumber(season.getId(), weekNumber)
                .filter(found -> found.getCompetition() == Competition.LEAGUE)
                .orElseThrow(() -> new MatchWeekNotFoundException(weekNumber));

        List<Match> matches = matchRepository.findByMatchWeekIdOrderById(week.getId());
        if (matches.stream().anyMatch(Match::isPlayed)) {
            throw new WeekAlreadyPlayedException(weekNumber);
        }
        if (season.isFinished()) {
            throw new SeasonFinishedException();
        }
        Integer nextWeek = matchRepository.findFirstUnplayedWeekNumber(season.getId()).orElse(weekNumber);
        if (!nextWeek.equals(weekNumber)) {
            throw new WeekOrderException(weekNumber, nextWeek);
        }

        simulateMatches(matches, Competition.LEAGUE);

        if (matchRepository.findFirstUnplayedWeekNumber(season.getId()).isEmpty()) {
            seasonEndService.finishSeason(season);
        }

        return new MatchWeekResponse(weekNumber, matches.stream().map(matchMapper::toMatchResponse).toList());
    }

    /**
     * Maçları simüle eder ve tüm sonuçları kaydeder: skor, olaylar, maç kadroları, istatistikler,
     * oyuncu ceza / sakatlık durumları. Moral ve güç yalnızca lig maçlarında değişir.
     */
    public void simulateMatches(List<Match> matches, Competition competition) {
        Set<Team> touchedTeams = new LinkedHashSet<>();
        matches.forEach(match -> {
            touchedTeams.add(match.getHomeTeam());
            touchedTeams.add(match.getAwayTeam());
        });
        Map<Long, List<Player>> squads = playerRepository
                .findByTeamIdInAndActiveTrue(touchedTeams.stream().map(Team::getId).toList()).stream()
                .collect(Collectors.groupingBy(player -> player.getTeam().getId()));

        List<MatchEvent> events = new ArrayList<>();
        List<MatchAppearance> appearances = new ArrayList<>();
        List<MatchTeamStats> stats = new ArrayList<>();
        for (Match match : matches) {
            List<Player> homeSquad = squads.getOrDefault(match.getHomeTeam().getId(), List.of());
            List<Player> awaySquad = squads.getOrDefault(match.getAwayTeam().getId(), List.of());

            MatchDetailGenerator.GeneratedDetail detail = simulateMatch(match, homeSquad, awaySquad,
                    competition == Competition.LEAGUE);
            updatePlayerStatuses(homeSquad, detail.events());
            updatePlayerStatuses(awaySquad, detail.events());

            detail.homeStats().setStrengthAfter(match.getHomeTeam().getStrength());
            detail.awayStats().setStrengthAfter(match.getAwayTeam().getStrength());
            events.addAll(detail.events());
            appearances.addAll(detail.appearances());
            stats.add(detail.homeStats());
            stats.add(detail.awayStats());
        }

        matchRepository.saveAll(matches);
        matchEventRepository.saveAll(events);
        matchAppearanceRepository.saveAll(appearances);
        matchTeamStatsRepository.saveAll(stats);
        teamRepository.saveAll(touchedTeams);
    }

    private MatchDetailGenerator.GeneratedDetail simulateMatch(Match match, List<Player> homeSquad,
            List<Player> awaySquad, boolean league) {
        Team home = match.getHomeTeam();
        Team away = match.getAwayTeam();

        ScoreSimulator.ExpectedGoals expected = scoreSimulator.expectedGoals(
                home.matchStrength(), home.getMorale(), away.matchStrength(), away.getMorale());
        ScoreSimulator.Probabilities probabilities = scoreSimulator.probabilities(expected);
        ScoreSimulator.SimulatedScore score = scoreSimulator.simulate(expected);

        match.setHomeScore(score.homeGoals());
        match.setAwayScore(score.awayGoals());
        match.setHomeWinProbability(probabilities.homeWinPercent());
        match.setDrawProbability(probabilities.drawPercent());
        match.setAwayWinProbability(probabilities.awayWinPercent());

        if (league) {
            applyMoraleChange(home, away, score.homeGoals(), score.awayGoals());
            home.changeStrength(strengthChange(points(score.homeGoals(), score.awayGoals()),
                    probabilities.homeExpectedPoints()));
            away.changeStrength(strengthChange(points(score.awayGoals(), score.homeGoals()),
                    probabilities.awayExpectedPoints()));
        }

        return matchDetailGenerator.generate(match,
                homeSquad.stream().filter(Player::isAvailable).toList(),
                awaySquad.stream().filter(Player::isAvailable).toList(),
                expected);
    }

    /**
     * Önce bu maçı cezası / sakatlığı yüzünden kaçıranların kalan maç sayısı bir azalır,
     * sonra bu maçtaki kırmızı kart, sarı kart birikimi ve sakatlıklar yazılır.
     */
    static void updatePlayerStatuses(List<Player> squad, List<MatchEvent> matchEvents) {
        for (Player player : squad) {
            if (!player.isAvailable()) {
                player.setSuspendedMatches(Math.max(0, player.getSuspendedMatches() - 1));
                player.setInjuredMatches(Math.max(0, player.getInjuredMatches() - 1));
            }
        }
        for (MatchEvent event : matchEvents) {
            Player player = event.getPlayer();
            if (!squad.contains(player)) {
                continue;
            }
            switch (event.getType()) {
                case RED_CARD -> player.setSuspendedMatches(player.getSuspendedMatches() + 1);
                case YELLOW_CARD -> {
                    player.setSeasonYellowCards(player.getSeasonYellowCards() + 1);
                    if (player.getSeasonYellowCards() % YELLOW_CARDS_PER_SUSPENSION == 0) {
                        player.setSuspendedMatches(player.getSuspendedMatches() + 1);
                    }
                }
                case INJURY -> player.setInjuredMatches(
                        ThreadLocalRandom.current().nextInt(1, MAX_INJURY_MATCHES + 1));
                case GOAL -> {
                }
            }
        }
    }

    private static int points(int goalsFor, int goalsAgainst) {
        if (goalsFor > goalsAgainst) {
            return 3;
        }
        return goalsFor == goalsAgainst ? 1 : 0;
    }

    private static int strengthChange(int actualPoints, double expectedPoints) {
        return (int) Math.round((actualPoints - expectedPoints) * STRENGTH_CHANGE_FACTOR);
    }

    private void applyMoraleChange(Team home, Team away, int homeGoals, int awayGoals) {
        if (homeGoals > awayGoals) {
            adjustMorale(home, MORALE_CHANGE);
            adjustMorale(away, -MORALE_CHANGE);
        } else if (homeGoals < awayGoals) {
            adjustMorale(away, MORALE_CHANGE);
            adjustMorale(home, -MORALE_CHANGE);
        }
    }

    private void adjustMorale(Team team, int delta) {
        team.setMorale(Math.min(MORALE_MAX, Math.max(MORALE_MIN, team.getMorale() + delta)));
    }
}
