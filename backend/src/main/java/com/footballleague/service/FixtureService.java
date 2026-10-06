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
import com.footballleague.entity.Match;
import com.footballleague.entity.MatchWeek;
import com.footballleague.entity.Season;
import com.footballleague.entity.Team;
import com.footballleague.exception.CupInProgressException;
import com.footballleague.exception.FixtureAlreadyGeneratedException;
import com.footballleague.exception.SeasonFinishedException;
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
public class FixtureService {

    private static final int MIN_TEAM_COUNT = 18;

    private final TeamRepository teamRepository;
    private final SeasonRepository seasonRepository;
    private final MatchWeekRepository matchWeekRepository;
    private final MatchRepository matchRepository;
    private final MatchEventRepository matchEventRepository;
    private final MatchTeamStatsRepository matchTeamStatsRepository;
    private final MatchAppearanceRepository matchAppearanceRepository;
    private final PlayerRepository playerRepository;
    private final RoundRobinScheduler roundRobinScheduler;
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

        List<Team> teams = teamRepository.findByActiveTrue();
        validateTeamCount(teams.size());

        Season season = seasonRepository.save(Season.builder()
                .seasonNumber(latest.map(previous -> previous.getSeasonNumber() + 1).orElse(1))
                .build());
        teams.forEach(team -> {
            team.setMorale(Team.INITIAL_MORALE);
            team.setSeasonStartStrength(team.getStrength());
            team.setLastStrengthChange(0);
        });
        resetPlayerStatuses(teams);

        Map<Long, Team> teamById = new LinkedHashMap<>();
        teams.forEach(team -> teamById.put(team.getId(), team));

        List<List<RoundRobinScheduler.Pairing>> firstLeg =
                roundRobinScheduler.generateSingleRoundRobin(new ArrayList<>(teamById.keySet()));

        List<MatchWeek> weeksToSave = new ArrayList<>();
        List<Match> matchesToSave = new ArrayList<>();
        int weekNumber = 1;

        for (List<RoundRobinScheduler.Pairing> weekPairings : firstLeg) {
            weekNumber = buildWeek(season, weekNumber, weekPairings, teamById, false, weeksToSave, matchesToSave);
        }
        for (List<RoundRobinScheduler.Pairing> weekPairings : firstLeg) {
            weekNumber = buildWeek(season, weekNumber, weekPairings, teamById, true, weeksToSave, matchesToSave);
        }

        matchWeekRepository.saveAll(weeksToSave);
        matchRepository.saveAll(matchesToSave);

        return getFixture(season.getId());
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

    /** Ceza, sakatlık ve sarı kart birikimi her sezon sıfırdan başlar. */
    private void resetPlayerStatuses(List<Team> teams) {
        playerRepository.findByTeamIdInAndActiveTrue(teams.stream().map(Team::getId).toList()).forEach(player -> {
            player.setSuspendedMatches(0);
            player.setInjuredMatches(0);
            player.setSeasonYellowCards(0);
        });
    }

    /** seasonId null ise güncel (en son) sezonun fikstürü döner; hiç sezon yoksa boş liste. */
    @Transactional(readOnly = true)
    public List<MatchWeekResponse> getFixture(Long seasonId) {
        Optional<Long> targetSeasonId = seasonId != null
                ? Optional.of(seasonId)
                : seasonRepository.findTopByOrderBySeasonNumberDesc().map(Season::getId);
        if (targetSeasonId.isEmpty()) {
            return List.of();
        }

        Map<Integer, List<MatchResponse>> matchesByWeek = new LinkedHashMap<>();
        for (Match match : matchRepository.findLeagueMatchesBySeason(targetSeasonId.get())) {
            matchesByWeek
                    .computeIfAbsent(match.getMatchWeek().getWeekNumber(), key -> new ArrayList<>())
                    .add(matchMapper.toMatchResponse(match));
        }

        return matchesByWeek.entrySet().stream()
                .map(entry -> new MatchWeekResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    private int buildWeek(Season season, int weekNumber, List<RoundRobinScheduler.Pairing> pairings,
            Map<Long, Team> teamById, boolean reverseVenue, List<MatchWeek> weeksToSave, List<Match> matchesToSave) {
        MatchWeek week = MatchWeek.builder().season(season).weekNumber(weekNumber).build();
        weeksToSave.add(week);

        for (RoundRobinScheduler.Pairing pairing : pairings) {
            Long homeId = reverseVenue ? pairing.awayTeamId() : pairing.homeTeamId();
            Long awayId = reverseVenue ? pairing.homeTeamId() : pairing.awayTeamId();

            matchesToSave.add(Match.builder()
                    .matchWeek(week)
                    .homeTeam(teamById.get(homeId))
                    .awayTeam(teamById.get(awayId))
                    .build());
        }
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
