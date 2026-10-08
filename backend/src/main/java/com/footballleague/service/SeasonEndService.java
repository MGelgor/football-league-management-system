package com.footballleague.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.footballleague.dto.StandingResponse;
import com.footballleague.entity.MatchAppearance;
import com.footballleague.entity.Player;
import com.footballleague.entity.Season;
import com.footballleague.entity.SeasonTeamChange;
import com.footballleague.entity.Team;
import com.footballleague.entity.TeamChangeType;
import com.footballleague.repository.MatchAppearanceRepository;
import com.footballleague.repository.PlayerRepository;
import com.footballleague.repository.SeasonRepository;
import com.footballleague.repository.SeasonTeamChangeRepository;
import com.footballleague.repository.TeamRepository;

import lombok.RequiredArgsConstructor;

/**
 * Lig bittiğinde (son hafta oynanınca) çalışır: şampiyon, sezon sonu takım gücü değişimi,
 * küme düşme / yükselme ve oyuncuların yaş + gelişim + emeklilik güncellemesi.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SeasonEndService {

    private final StandingsService standingsService;
    private final TeamRepository teamRepository;
    private final SeasonRepository seasonRepository;
    private final SeasonTeamChangeRepository seasonTeamChangeRepository;
    private final PlayerRepository playerRepository;
    private final MatchAppearanceRepository matchAppearanceRepository;
    private final TeamService teamService;
    private final PlayerDevelopment playerDevelopment;
    private final EconomyService economyService;
    private final TransferService transferService;
    private final CareerService careerService;

    public void finishSeason(Season season) {
        List<StandingResponse> standings = standingsService.getStandings(season.getId());
        Map<Long, Team> teamById = teamRepository.findAllById(standings.stream().map(StandingResponse::teamId).toList())
                .stream().collect(Collectors.toMap(Team::getId, Function.identity()));

        for (StandingResponse row : standings) {
            teamById.get(row.teamId()).changeStrength(seasonEndStrengthChange(row.rank(), standings.size()));
        }
        season.setFinished(true);
        season.setChampion(teamById.get(standings.getFirst().teamId()));
        seasonRepository.save(season);

        economyService.awardLeaguePrizes(season, standings, teamById);
        careerService.onLeagueFinished(season, standings, StandingsService.relegationZone(standings,
                teamById.values().stream().filter(Team::isBigFour).map(Team::getId).collect(Collectors.toSet())));
        List<StandingResponse> second = standingsService.getStandings(season.getId(), 2);
        Map<Long, Team> secondById = teamRepository.findAllById(second.stream().map(StandingResponse::teamId).toList())
                .stream().collect(Collectors.toMap(Team::getId, Function.identity()));
        for (StandingResponse row : second) {
            secondById.get(row.teamId()).changeStrength(seasonEndStrengthChange(row.rank(), second.size()));
        }
        if (!second.isEmpty()) {
            season.setSecondLeagueChampion(secondById.get(second.getFirst().teamId()));
        }
        developPlayers(season, teamRepository.findByActiveTrue());
        relegateAndPromote(season, standings, teamById, second, secondById);
        economyService.processContracts(season, teamRepository.findByActiveTrue());
        transferService.openWindow(season);
        economyService.ensureEconomy();
    }

    static int seasonEndStrengthChange(int rank, int teamCount) {
        if (rank == 1) {
            return 4;
        }
        if (rank <= 4) {
            return 2;
        }
        if (rank > teamCount - 3) {
            return -3;
        }
        return rank <= teamCount / 2 ? 1 : -1;
    }

    /**
     * 1. Lig'in son 3'ü (4 büyükler hariç) 2. Lig'e düşer, 2. Lig'in ilk 3'ü çıkar; 2. Lig'in son 3'ü lig
     * sisteminden ayrılır (arşivlenir), yerlerine 3 yeni takım katılır.
     */
    private void relegateAndPromote(Season season, List<StandingResponse> standings, Map<Long, Team> teamById,
            List<StandingResponse> second, Map<Long, Team> secondById) {
        Set<Long> bigFour = teamById.values().stream().filter(Team::isBigFour).map(Team::getId).collect(Collectors.toSet());
        List<Long> relegatedIds = StandingsService.relegationZone(standings, bigFour);

        List<SeasonTeamChange> changes = new ArrayList<>();
        for (Long teamId : relegatedIds) {
            Team team = teamById.get(teamId);
            team.setDivision(2);
            changes.add(SeasonTeamChange.builder().season(season).team(team).type(TeamChangeType.RELEGATED).build());
        }
        second.stream().limit(relegatedIds.size()).forEach(row -> {
            Team team = secondById.get(row.teamId());
            team.setDivision(1);
            changes.add(SeasonTeamChange.builder().season(season).team(team).type(TeamChangeType.PROMOTED).build());
        });
        List<Long> droppedOut = StandingsService.bottom(second, StandingsService.RELEGATION_COUNT);
        for (Long teamId : droppedOut) {
            Team team = secondById.get(teamId);
            team.setActive(false);
            changes.add(SeasonTeamChange.builder().season(season).team(team).type(TeamChangeType.DROPPED_OUT).build());
        }
        for (Team joined : teamService.createSecondDivisionTeams(droppedOut.size())) {
            changes.add(SeasonTeamChange.builder().season(season).team(joined).type(TeamChangeType.JOINED).build());
        }
        seasonTeamChangeRepository.saveAll(changes);
    }

    /**
     * Herkes bir yaş büyür, gücü yaşa ve sezon reytingine göre değişir. Emekli olanın yeri transfer penceresinde
     * (altyapı, serbest oyuncu, transfer) doldurulur.
     */
    private void developPlayers(Season season, List<Team> teams) {
        Map<Long, Double> averageRating = matchAppearanceRepository.findLeagueBySeasonWithPlayers(season.getId()).stream()
                .collect(Collectors.groupingBy(appearance -> appearance.getPlayer().getId(),
                        Collectors.averagingDouble(MatchAppearance::getRating)));

        Map<Long, List<Player>> squads = playerRepository
                .findByTeamIdInAndActiveTrue(teams.stream().map(Team::getId).toList()).stream()
                .collect(Collectors.groupingBy(player -> player.getTeam().getId()));

        for (Team team : teams) {
            for (Player player : squads.getOrDefault(team.getId(), List.of())) {
                player.changeStrength(playerDevelopment.strengthChange(player.getAge(), averageRating.get(player.getId())));
                player.setAge(player.getAge() + 1);
                if (playerDevelopment.retires(player.getAge())) {
                    player.setActive(false);
                }
            }
        }
    }
}
