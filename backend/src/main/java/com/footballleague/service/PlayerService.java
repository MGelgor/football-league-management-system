package com.footballleague.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.footballleague.dto.PlayerProfileResponse;
import com.footballleague.dto.PlayerRequest;
import com.footballleague.dto.PlayerResponse;
import com.footballleague.dto.PlayerStatsResponse;
import com.footballleague.entity.Competition;
import com.footballleague.entity.Match;
import com.footballleague.entity.MatchAppearance;
import com.footballleague.entity.MatchEvent;
import com.footballleague.entity.MatchEventType;
import com.footballleague.entity.MatchWeek;
import com.footballleague.entity.Player;
import com.footballleague.entity.Season;
import com.footballleague.entity.Team;
import com.footballleague.exception.PlayerNotFoundException;
import com.footballleague.exception.SeasonNotFoundException;
import com.footballleague.exception.TeamNotFoundException;
import com.footballleague.repository.MatchAppearanceRepository;
import com.footballleague.repository.MatchEventRepository;
import com.footballleague.repository.PlayerRepository;
import com.footballleague.repository.SeasonRepository;
import com.footballleague.repository.TeamRepository;
import com.footballleague.repository.TransferRepository;

import lombok.RequiredArgsConstructor;

/**
 * Oyuncu istatistikleri ayrı bir tabloda değil, her maçta kaydedilen olaylardan (match_events) ve
 * maç kadrolarından (match_appearances) sayılır: hepsi maç oynandığı anda kalıcı olarak saklanır.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PlayerService {

    private static final int MAX_SQUAD_SIZE = 30;

    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final SeasonRepository seasonRepository;
    private final MatchEventRepository matchEventRepository;
    private final MatchAppearanceRepository matchAppearanceRepository;
    private final EconomyService economyService;
    private final TransferRepository transferRepository;

    /** Takım kadrosu (emekliler hariç): güncel sezon lig ve kariyer istatistikleriyle. */
    @Transactional(readOnly = true)
    public List<PlayerResponse> getSquad(Long teamId) {
        if (!teamRepository.existsById(teamId)) {
            throw new TeamNotFoundException(teamId);
        }
        Optional<Season> current = seasonRepository.findTopByOrderBySeasonNumberDesc();
        Map<Long, PlayerTotals> season = current
                .map(s -> PlayerTotals.of(matchEventRepository.findLeagueEventsByTeamAndSeason(teamId, s.getId()),
                        matchAppearanceRepository.findLeagueByTeamAndSeason(teamId, s.getId())))
                .orElse(Map.of());
        Map<Long, PlayerTotals> career = PlayerTotals.of(matchEventRepository.findByTeamId(teamId),
                matchAppearanceRepository.findByTeamId(teamId));

        return playerRepository.findByTeamIdAndActiveTrueOrderByShirtNumber(teamId).stream()
                .map(player -> toResponse(player, PlayerTotals.get(season, player.getId()),
                        PlayerTotals.get(career, player.getId())))
                .toList();
    }

    public PlayerResponse addPlayer(Long teamId, PlayerRequest request) {
        Team team = teamRepository.findById(teamId)
                .filter(Team::isActive)
                .orElseThrow(() -> new TeamNotFoundException(teamId));
        List<Player> squad = playerRepository.findByTeamIdAndActiveTrueOrderByShirtNumber(teamId);
        if (squad.size() >= MAX_SQUAD_SIZE) {
            throw new IllegalArgumentException("Bir takımda en fazla " + MAX_SQUAD_SIZE + " oyuncu olabilir");
        }
        if (squad.stream().anyMatch(player -> player.getShirtNumber().equals(request.shirtNumber()))) {
            throw duplicateShirtNumber(request.shirtNumber());
        }

        Player player = toPlayer(team, request);
        economyService.signNewPlayers(List.of(player));
        player = playerRepository.save(player);
        return findInSquad(teamId, player.getId());
    }

    public PlayerResponse updatePlayer(Long id, PlayerRequest request) {
        Player player = playerRepository.findById(id)
                .filter(found -> found.isActive() && found.getTeam() != null)
                .orElseThrow(() -> new PlayerNotFoundException(id));
        Long teamId = player.getTeam().getId();
        if (playerRepository.existsByTeamIdAndShirtNumberAndActiveTrueAndIdNot(teamId, request.shirtNumber(), id)) {
            throw duplicateShirtNumber(request.shirtNumber());
        }

        player.setName(request.name().trim());
        player.setPosition(request.position());
        player.setShirtNumber(request.shirtNumber());
        player.setStrength(request.strength());
        player.setAge(request.age());

        return findInSquad(teamId, id);
    }

    /** Sezonun lig istatistikleri: en az bir maçta oynayan oyuncular, gol → asist → ad sırasıyla. */
    @Transactional(readOnly = true)
    public List<PlayerStatsResponse> getSeasonStats(Long seasonId) {
        Optional<Season> season = seasonId == null
                ? seasonRepository.findTopByOrderBySeasonNumberDesc()
                : Optional.of(seasonRepository.findById(seasonId).orElseThrow(() -> new SeasonNotFoundException(seasonId)));
        if (season.isEmpty()) {
            return List.of();
        }

        List<MatchEvent> events = matchEventRepository.findLeagueEventsBySeasonWithPlayers(season.get().getId());
        List<MatchAppearance> appearances = matchAppearanceRepository.findLeagueBySeasonWithPlayers(season.get().getId());
        Map<Long, PlayerTotals> totals = PlayerTotals.of(events, appearances);
        Map<Long, Player> players = new LinkedHashMap<>();
        // Oyuncunun o sezon oynadığı son takım (sezon içinde transfer olmuş ya da sonra serbest kalmış olabilir)
        Map<Long, Team> teams = new HashMap<>();
        appearances.forEach(appearance -> {
            players.putIfAbsent(appearance.getPlayer().getId(), appearance.getPlayer());
            teams.put(appearance.getPlayer().getId(), appearance.getTeam());
        });

        return players.values().stream()
                .map(player -> {
                    PlayerTotals t = PlayerTotals.get(totals, player.getId());
                    Team team = teams.get(player.getId());
                    return new PlayerStatsResponse(player.getId(), player.getName(), player.getPosition(),
                            team.getId(), team.getName(), t.appearances, t.minutes, t.goals,
                            t.assists, t.yellowCards, t.redCards, t.averageRating(), t.playerOfTheMatch, t.ownGoals);
                })
                .sorted(Comparator.comparingInt(PlayerStatsResponse::goals).reversed()
                        .thenComparing(Comparator.comparingInt(PlayerStatsResponse::assists).reversed())
                        .thenComparing(PlayerStatsResponse::playerName))
                .toList();
    }

    /** Oyuncu sayfası: emekli ya da küme düşmüş takımın oyuncusu da görüntülenebilir. */
    @Transactional(readOnly = true)
    public PlayerProfileResponse getProfile(Long id) {
        Player player = playerRepository.findById(id).orElseThrow(() -> new PlayerNotFoundException(id));
        List<MatchEvent> events = matchEventRepository.findByPlayerInvolved(id);
        List<MatchAppearance> appearances = matchAppearanceRepository.findByPlayerIdWithMatch(id);

        // Sezon + turnuva bazında satırlar: önce sezon, aynı sezonda önce lig
        Comparator<SeasonKey> order = Comparator.comparingInt(SeasonKey::seasonNumber)
                .thenComparing(SeasonKey::competition);
        Map<SeasonKey, List<MatchAppearance>> appearancesByKey = appearances.stream()
                .collect(Collectors.groupingBy(appearance -> SeasonKey.of(appearance.getMatch().getMatchWeek())));
        Map<SeasonKey, List<MatchEvent>> eventsByKey = events.stream()
                .collect(Collectors.groupingBy(event -> SeasonKey.of(event.getMatch().getMatchWeek())));

        List<PlayerProfileResponse.SeasonLine> seasons = appearancesByKey.keySet().stream()
                .sorted(order)
                .map(key -> {
                    PlayerTotals t = PlayerTotals.get(PlayerTotals.of(
                            eventsByKey.getOrDefault(key, List.of()), appearancesByKey.get(key)), id);
                    return new PlayerProfileResponse.SeasonLine(key.seasonNumber(), key.competition(), t.appearances,
                            t.minutes, t.goals, t.assists, t.yellowCards, t.redCards, t.averageRating(),
                            t.playerOfTheMatch);
                })
                .toList();

        List<PlayerProfileResponse.GoalLine> goals = new ArrayList<>();
        for (MatchEvent event : events) {
            if (event.getType() != MatchEventType.GOAL) {
                continue;
            }
            boolean scored = event.getPlayer().getId().equals(id);
            Player partner = scored ? event.getAssistPlayer() : event.getPlayer();
            goals.add(goalLine(event, event.getTeam().getId(), scored ? "GOAL" : "ASSIST",
                    partner != null ? partner.getName() : null));
        }

        List<PlayerProfileResponse.InjuryLine> injuries = events.stream()
                .filter(event -> event.getType() == MatchEventType.INJURY && event.getPlayer().getId().equals(id))
                .map(event -> {
                    MatchWeek week = event.getMatch().getMatchWeek();
                    return new PlayerProfileResponse.InjuryLine(event.getMatch().getId(),
                            week.getSeason().getSeasonNumber(), week.getCompetition(), week.getWeekNumber(),
                            week.getCupRound(), event.getMinute(), event.getInjuryMatches());
                })
                .toList();

        Team team = player.getTeam();
        return new PlayerProfileResponse(player.getId(), player.getName(), player.getPosition(),
                player.getShirtNumber(), player.getStrength(), player.getAge(), player.getLastStrengthChange(),
                !player.isActive(), player.getSuspendedMatches(), player.getInjuredMatches(),
                team != null ? team.getId() : null, team != null ? team.getName() : null,
                team != null && team.isActive(), player.ratingHistory(), roundForm(player.form()),
                player.getInjurySeverity(), seasons, goals, injuries,
                transferRepository.findByPlayerId(id).stream().map(TransferService::toResponse).toList());
    }

    private record SeasonKey(int seasonNumber, Competition competition) {

        static SeasonKey of(MatchWeek week) {
            return new SeasonKey(week.getSeason().getSeasonNumber(), week.getCompetition());
        }
    }

    private static PlayerProfileResponse.GoalLine goalLine(MatchEvent event, Long ownTeamId, String type,
            String partnerName) {
        Match match = event.getMatch();
        boolean home = match.getHomeTeam().getId().equals(ownTeamId);
        MatchWeek week = match.getMatchWeek();
        return new PlayerProfileResponse.GoalLine(match.getId(), week.getSeason().getSeasonNumber(),
                week.getCompetition(), week.getWeekNumber(), week.getCupRound(),
                home ? match.getAwayTeam().getName() : match.getHomeTeam().getName(), home,
                match.getHomeScore() + " - " + match.getAwayScore(), event.getMinute(), type, partnerName);
    }

    /** Takım oluştururken kullanıcının girdiği oyuncuları entity'ye çevirir (forma numaraları tekil olmalı). */
    static List<Player> toPlayers(Team team, List<PlayerRequest> requests) {
        Map<Integer, Long> numberCounts = requests.stream()
                .collect(Collectors.groupingBy(PlayerRequest::shirtNumber, Collectors.counting()));
        numberCounts.forEach((number, count) -> {
            if (count > 1) {
                throw duplicateShirtNumber(number);
            }
        });
        return requests.stream().map(request -> toPlayer(team, request)).toList();
    }

    private static Player toPlayer(Team team, PlayerRequest request) {
        return Player.builder()
                .team(team)
                .name(request.name().trim())
                .position(request.position())
                .shirtNumber(request.shirtNumber())
                .strength(request.strength())
                .age(request.age())
                .build();
    }

    private static PlayerResponse toResponse(Player player, PlayerTotals season, PlayerTotals career) {
        return new PlayerResponse(player.getId(), player.getName(), player.getPosition(), player.getShirtNumber(),
                player.getStrength(), player.getAge(), player.getLastStrengthChange(), player.getSuspendedMatches(),
                player.getInjuredMatches(), season.appearances, season.minutes, season.goals, season.assists,
                season.yellowCards, season.redCards, season.averageRating(), season.playerOfTheMatch,
                career.appearances, career.goals, career.assists, roundForm(player.form()), player.fatigue(),
                player.getInjurySeverity(), Economy.marketValue(player), player.getWage(), player.getContractUntil());
    }

    private static double roundForm(double form) {
        return Math.round(form * 100) / 100.0;
    }

    private PlayerResponse findInSquad(Long teamId, Long playerId) {
        return getSquad(teamId).stream()
                .filter(response -> response.id().equals(playerId))
                .findFirst()
                .orElseThrow();
    }

    private static IllegalArgumentException duplicateShirtNumber(Integer number) {
        return new IllegalArgumentException(number + " numaralı forma takımda başka bir oyuncuda");
    }
}
