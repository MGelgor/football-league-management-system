package com.footballleague.service;

import java.util.ArrayList;
import java.util.HashSet;
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
    private final SquadGenerator squadGenerator;
    private final PlayerDevelopment playerDevelopment;

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

        developPlayers(season, teamById.values().stream().filter(Team::isActive).toList());
        relegateAndPromote(season, standings, teamById);
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

    private void relegateAndPromote(Season season, List<StandingResponse> standings, Map<Long, Team> teamById) {
        Set<Long> bigFour = teamById.values().stream().filter(Team::isBigFour).map(Team::getId).collect(Collectors.toSet());
        List<Long> relegatedIds = StandingsService.relegationZone(standings, bigFour);

        List<SeasonTeamChange> changes = new ArrayList<>();
        for (Long teamId : relegatedIds) {
            Team team = teamById.get(teamId);
            team.setActive(false);
            changes.add(SeasonTeamChange.builder().season(season).team(team).type(TeamChangeType.RELEGATED).build());
        }
        for (Team promoted : teamService.createPromotedTeams(relegatedIds.size())) {
            changes.add(SeasonTeamChange.builder().season(season).team(promoted).type(TeamChangeType.PROMOTED).build());
        }
        seasonTeamChangeRepository.saveAll(changes);
    }

    /** Herkes bir yaş büyür, gücü yaşa ve sezon reytingine göre değişir; emekli olanın yerine genç oyuncu gelir. */
    private void developPlayers(Season season, List<Team> teams) {
        Map<Long, Double> averageRating = matchAppearanceRepository.findLeagueBySeasonWithPlayers(season.getId()).stream()
                .collect(Collectors.groupingBy(appearance -> appearance.getPlayer().getId(),
                        Collectors.averagingDouble(MatchAppearance::getRating)));

        Map<Long, List<Player>> squads = playerRepository
                .findByTeamIdInAndActiveTrue(teams.stream().map(Team::getId).toList()).stream()
                .collect(Collectors.groupingBy(player -> player.getTeam().getId()));

        List<Player> youth = new ArrayList<>();
        for (Team team : teams) {
            List<Player> squad = squads.getOrDefault(team.getId(), List.of());
            List<Player> retired = new ArrayList<>();
            for (Player player : squad) {
                player.changeStrength(playerDevelopment.strengthChange(player.getAge(), averageRating.get(player.getId())));
                player.setAge(player.getAge() + 1);
                if (playerDevelopment.retires(player.getAge())) {
                    player.setActive(false);
                    retired.add(player);
                }
            }
            Set<String> usedNames = squad.stream().map(Player::getName).collect(Collectors.toCollection(HashSet::new));
            Set<Integer> usedNumbers = squad.stream().filter(Player::isActive).map(Player::getShirtNumber)
                    .collect(Collectors.toCollection(HashSet::new));
            for (Player retiree : retired) {
                Player newcomer = squadGenerator.youthPlayer(team, retiree.getPosition(), usedNames, usedNumbers);
                usedNumbers.add(newcomer.getShirtNumber());
                youth.add(newcomer);
            }
        }
        playerRepository.saveAll(youth);
    }
}
