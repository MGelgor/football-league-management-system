package com.footballleague.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.footballleague.dto.MatchResponse;
import com.footballleague.dto.MatchWeekResponse;
import com.footballleague.entity.Competition;
import com.footballleague.entity.Match;
import com.footballleague.entity.MatchWeek;
import com.footballleague.entity.Season;
import com.footballleague.entity.Team;
import com.footballleague.exception.CupInProgressException;
import com.footballleague.exception.FixtureAlreadyGeneratedException;
import com.footballleague.exception.SeasonFinishedException;
import com.footballleague.repository.MatchAppearanceRepository;
import com.footballleague.repository.MatchEventRepository;
import com.footballleague.repository.MatchLineupRepository;
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
public class FixtureService {

    private static final int MIN_TEAM_COUNT = 18;
    // Yaz arasında sakatlıkların iyileştiği maç sayısı
    static final int OFF_SEASON_RECOVERY = 4;

    private final TeamRepository teamRepository;
    private final SeasonRepository seasonRepository;
    private final MatchWeekRepository matchWeekRepository;
    private final MatchRepository matchRepository;
    private final MatchEventRepository matchEventRepository;
    private final MatchTeamStatsRepository matchTeamStatsRepository;
    private final MatchAppearanceRepository matchAppearanceRepository;
    private final PlayerRepository playerRepository;
    private final RoundRobinScheduler roundRobinScheduler;
    private final RefereeService refereeService;
    private final EconomyService economyService;
    private final TransferService transferService;
    private final MatchLineupRepository matchLineupRepository;
    private final CareerService careerService;
    private final TeamService teamService;
    private final MatchMapper matchMapper;

    /** Yeni sezon açar ve fikstürünü üretir. Önceki sezon bitmemişse hata verir. */
    public List<MatchWeekResponse> generateFixture() {
        Optional<Season> latest = seasonRepository.findTopByOrderBySeasonNumberDesc();
        if (latest.isPresent() && !latest.get().isFinished()) {
            throw new FixtureAlreadyGeneratedException();
        }
        if (latest.isPresent() && latest.get().getCupWinner() == null
                && !matchRepository.findCupMatchesBySeason(latest.get().getId()).isEmpty()) {
            throw new CupInProgressException();
        }

        // Yeni sezonun fikstürü transfer penceresini kapatır
        transferService.closeWindow();
        List<Team> teams = teamRepository.findByActiveTrueAndDivision(1);
        validateTeamCount(teams.size());
        List<Team> secondDivision = teamService.ensureSecondDivision(teams.size());
        economyService.ensureEconomy();

        Season season = seasonRepository.save(Season.builder()
                .seasonNumber(latest.map(previous -> previous.getSeasonNumber() + 1).orElse(1))
                .build());
        List<Team> allTeams = new ArrayList<>(teams);
        allTeams.addAll(secondDivision);
        allTeams.forEach(team -> {
            team.setMorale(Team.INITIAL_MORALE);
            team.setSeasonStartStrength(team.getStrength());
            team.setLastStrengthChange(0);
        });
        startPlayerSeason(allTeams);

        List<MatchWeek> weeksToSave = new ArrayList<>();
        List<Match> matchesToSave = new ArrayList<>();
        scheduleLeague(season, teams, Competition.LEAGUE, weeksToSave, matchesToSave);
        scheduleLeague(season, secondDivision, Competition.SECOND_LEAGUE, weeksToSave, matchesToSave);

        matchWeekRepository.saveAll(weeksToSave);
        matchRepository.saveAll(matchesToSave);
        careerService.onSeasonStart(season);

        return getFixture(season.getId());
    }

    /** Çift devreli round-robin; 2. Lig haftaları 201'den başlar. */
    private void scheduleLeague(Season season, List<Team> teams, Competition competition, List<MatchWeek> weeksToSave,
            List<Match> matchesToSave) {
        if (teams.isEmpty()) {
            return;
        }
        Map<Long, Team> teamById = new LinkedHashMap<>();
        teams.forEach(team -> teamById.put(team.getId(), team));
        List<List<RoundRobinScheduler.Pairing>> firstLeg =
                roundRobinScheduler.generateSingleRoundRobin(new ArrayList<>(teamById.keySet()));
        int weekNumber = competition == Competition.SECOND_LEAGUE ? Competition.SECOND_LEAGUE_WEEK_OFFSET + 1 : 1;
        for (List<RoundRobinScheduler.Pairing> weekPairings : firstLeg) {
            weekNumber = buildWeek(season, competition, weekNumber, weekPairings, teamById, false, weeksToSave,
                    matchesToSave);
        }
        for (List<RoundRobinScheduler.Pairing> weekPairings : firstLeg) {
            weekNumber = buildWeek(season, competition, weekNumber, weekPairings, teamById, true, weeksToSave,
                    matchesToSave);
        }
    }

    /**
     * Devam eden sezonu iptal eder: maçlar, olaylar, istatistikler ve haftalar silinir,
     * takımların gücü ve morali sezon başındaki değere döner. Tamamlanmış sezonlar arşivde kalır.
     */
    public void resetFixture() {
        Optional<Season> latest = seasonRepository.findTopByOrderBySeasonNumberDesc();
        if (latest.isEmpty()) {
            return;
        }
        Season season = latest.get();
        if (season.isFinished()) {
            throw new SeasonFinishedException();
        }

        economyService.revertSeason(season);
        matchLineupRepository.deleteBySeasonId(season.getId());
        matchEventRepository.deleteBySeasonId(season.getId());
        matchAppearanceRepository.deleteBySeasonId(season.getId());
        matchTeamStatsRepository.deleteBySeasonId(season.getId());
        matchRepository.deleteBySeasonId(season.getId());
        matchWeekRepository.deleteBySeasonId(season.getId());
        seasonRepository.delete(season);

        List<Team> teams = teamRepository.findByActiveTrue();
        teams.forEach(team -> {
            team.setMorale(Team.INITIAL_MORALE);
            if (team.getSeasonStartStrength() != null) {
                team.setStrength(team.getSeasonStartStrength());
            }
            team.setLastStrengthChange(0);
        });
        resetPlayerStatuses(teams);
    }

    /**
     * Yeni sezon: ceza ve sarı kart birikimi sıfırlanır, yaz arasında sakatlıklar OFF_SEASON_RECOVERY maç kadar
     * iyileşir (uzun sakatlıklar yeni sezona taşar), herkes dinlenmiş başlar.
     */
    private void startPlayerSeason(List<Team> teams) {
        playerRepository.findByTeamIdInAndActiveTrue(teams.stream().map(Team::getId).toList()).forEach(player -> {
            player.setSuspendedMatches(0);
            player.setSeasonYellowCards(0);
            player.setInjuredMatches(Math.max(0, player.getInjuredMatches() - OFF_SEASON_RECOVERY));
            if (player.getInjuredMatches() == 0) {
                player.setInjurySeverity(null);
            }
            player.setConsecutiveStarts(0);
        });
    }

    /** Sezon iptalinde oyuncu durumları tamamen sıfırlanır. */
    private void resetPlayerStatuses(List<Team> teams) {
        playerRepository.findByTeamIdInAndActiveTrue(teams.stream().map(Team::getId).toList()).forEach(player -> {
            player.setSuspendedMatches(0);
            player.setInjuredMatches(0);
            player.setInjurySeverity(null);
            player.setSeasonYellowCards(0);
            player.setConsecutiveStarts(0);
        });
    }

    @Transactional(readOnly = true)
    public List<MatchWeekResponse> getFixture(Long seasonId) {
        return getFixture(seasonId, 1);
    }

    /**
     * seasonId null ise güncel (en son) sezonun fikstürü döner; hiç sezon yoksa boş liste. division 2: 2. Lig
     * (hafta numaraları 1..N olarak döner).
     */
    @Transactional(readOnly = true)
    public List<MatchWeekResponse> getFixture(Long seasonId, int division) {
        Optional<Long> targetSeasonId = seasonId != null
                ? Optional.of(seasonId)
                : seasonRepository.findTopByOrderBySeasonNumberDesc().map(Season::getId);
        if (targetSeasonId.isEmpty()) {
            return List.of();
        }

        Competition competition = Competition.ofDivision(division);
        Map<Integer, List<MatchResponse>> matchesByWeek = new LinkedHashMap<>();
        for (Match match : matchRepository.findBySeasonAndCompetition(targetSeasonId.get(), competition)) {
            matchesByWeek
                    .computeIfAbsent(competition.displayWeek(match.getMatchWeek().getWeekNumber()), key -> new ArrayList<>())
                    .add(matchMapper.toMatchResponse(match));
        }

        return matchesByWeek.entrySet().stream()
                .map(entry -> new MatchWeekResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    private int buildWeek(Season season, Competition competition, int weekNumber,
            List<RoundRobinScheduler.Pairing> pairings, Map<Long, Team> teamById, boolean reverseVenue,
            List<MatchWeek> weeksToSave, List<Match> matchesToSave) {
        MatchWeek week = MatchWeek.builder().season(season).weekNumber(weekNumber).competition(competition).build();
        weeksToSave.add(week);

        List<Match> weekMatches = new ArrayList<>();
        for (RoundRobinScheduler.Pairing pairing : pairings) {
            Long homeId = reverseVenue ? pairing.awayTeamId() : pairing.homeTeamId();
            Long awayId = reverseVenue ? pairing.homeTeamId() : pairing.awayTeamId();

            weekMatches.add(Match.builder()
                    .matchWeek(week)
                    .homeTeam(teamById.get(homeId))
                    .awayTeam(teamById.get(awayId))
                    .build());
        }
        refereeService.assign(weekMatches);
        matchesToSave.addAll(weekMatches);
        return weekNumber + 1;
    }

    private void validateTeamCount(int count) {
        if (count < MIN_TEAM_COUNT) {
            throw new IllegalArgumentException(
                    "Fikstür oluşturmak için en az " + MIN_TEAM_COUNT + " takım gerekli (mevcut: " + count + ")");
        }
        if (count % 2 != 0) {
            throw new IllegalArgumentException("Takım sayısı çift olmalı (mevcut: " + count + ")");
        }
    }
}
