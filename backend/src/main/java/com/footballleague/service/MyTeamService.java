package com.footballleague.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.footballleague.dto.LineupRequest;
import com.footballleague.dto.LineupResponse;
import com.footballleague.dto.MyTeamRequest;
import com.footballleague.dto.MyTeamResponse;
import com.footballleague.dto.PlayerResponse;
import com.footballleague.dto.StandingResponse;
import com.footballleague.entity.Competition;
import com.footballleague.entity.ManagerProfile;
import com.footballleague.entity.Match;
import com.footballleague.entity.MatchLineup;
import com.footballleague.entity.Player;
import com.footballleague.entity.Position;
import com.footballleague.entity.Season;
import com.footballleague.entity.Team;
import com.footballleague.exception.ManagerModeException;
import com.footballleague.exception.TeamNotFoundException;
import com.footballleague.repository.ManagerProfileRepository;
import com.footballleague.repository.MatchLineupRepository;
import com.footballleague.repository.MatchRepository;
import com.footballleague.repository.PlayerRepository;
import com.footballleague.repository.SeasonRepository;
import com.footballleague.repository.TeamRepository;

import lombok.RequiredArgsConstructor;

/**
 * "Takımımı Yönet" modu: kullanıcı bir takımın menajeri olur. Takımın maçı geldiğinde hafta (ya da kupa turu)
 * kullanıcıyı bekler: kadro (ilk 11, diziliş, stil, kaptan, penaltıcı) seçilir, maç canlı (devre arası
 * müdahaleyle, InteractiveMatchService) ya da hızlı oynatılır. Mod kapalıyken sistem tamamen otomatik çalışır.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MyTeamService {

    private static final int RECENT_RESULTS = 5;
    // Puan durumunda takımın üstünde / altında gösterilen sıra sayısı
    private static final int STANDINGS_AROUND = 2;

    private final ManagerProfileRepository managerProfileRepository;
    private final TeamRepository teamRepository;
    private final SeasonRepository seasonRepository;
    private final MatchRepository matchRepository;
    private final MatchLineupRepository matchLineupRepository;
    private final PlayerRepository playerRepository;
    private final ManagerService managerService;
    private final StandingsService standingsService;
    private final PlayerService playerService;
    private final MatchMapper matchMapper;
    private final TransferService transferService;
    private final MatchSimulationService matchSimulationService;
    private final CupService cupService;
    private final CareerService careerService;
    private final InboxService inboxService;

    @Transactional(readOnly = true)
    public MyTeamResponse dashboard() {
        Optional<ManagerProfile> anyProfile = managerProfileRepository.findFirstByOrderByIdAsc();
        Optional<ManagerProfile> profile = anyProfile.filter(found -> found.getTeam() != null);
        if (profile.isEmpty()) {
            return new MyTeamResponse(false, anyProfile.map(ManagerProfile::getName).orElse(null), null, null, null,
                    null, null, List.of(), List.of(), List.of(), false, 0, null, null, inboxService.unreadCount(),
                    anyProfile.isPresent());
        }
        Team team = profile.get().getTeam();
        Optional<Season> season = seasonRepository.findTopByOrderBySeasonNumberDesc();
        Optional<Match> next = nextMatch(team);

        StandingResponse standing = null;
        List<StandingResponse> around = List.of();
        if (season.isPresent()) {
            List<StandingResponse> standings = standingsService.getStandings(season.get().getId());
            standing = standings.stream().filter(row -> row.teamId().equals(team.getId())).findFirst().orElse(null);
            if (standing != null) {
                int index = standing.rank() - 1;
                around = standings.subList(Math.max(0, index - STANDINGS_AROUND),
                        Math.min(standings.size(), index + STANDINGS_AROUND + 1));
            }
        }
        List<PlayerResponse> unavailable = playerService.getSquad(team.getId()).stream()
                .filter(player -> player.suspendedMatches() > 0 || player.injuredMatches() > 0)
                .toList();

        return new MyTeamResponse(true, profile.get().getName(), TeamService.toResponse(team),
                season.map(Season::getSeasonNumber).orElse(null), status(team, season, next),
                next.map(match -> nextMatchResponse(match, team)).orElse(null), standing, around,
                recentResults(team, season), unavailable, transferService.window().open(),
                profile.get().getConfidence(), profile.get().getTargetRank(), profile.get().getCupTarget(),
                inboxService.unreadCount(), false);
    }

    /** Takımı devral: takımın yapay zekâ hocası görevden ayrılır, kullanıcı takımın hocası olur. */
    public MyTeamResponse start(MyTeamRequest request) {
        Team team = teamRepository.findById(request.teamId()).filter(Team::isActive)
                .orElseThrow(() -> new TeamNotFoundException(request.teamId()));
        ManagerProfile profile = managerProfileRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> ManagerProfile.builder().build());
        if (profile.getTeam() != null && profile.getTeam().getId().equals(team.getId())) {
            profile.setName(request.managerName().trim());
            return dashboard();
        }
        if (profile.getTeam() != null) {
            managerService.hireFor(profile.getTeam());
        }
        profile.setName(request.managerName().trim());
        profile.setTeam(team);
        profile.setStartedSeason(seasonRepository.findTopByOrderBySeasonNumberDesc()
                .map(season -> season.isFinished() ? season.getSeasonNumber() + 1 : season.getSeasonNumber())
                .orElse(1));
        team.setManager(null);
        managerProfileRepository.save(profile);
        careerService.startSpell(profile, team);
        return dashboard();
    }

    /** Modu kapat: takıma yeniden yapay zekâ hocası atanır; sistem tamamen otomatik çalışır. */
    public void stop() {
        managerProfileRepository.findFirstByOrderByIdAsc().ifPresent(profile -> {
            if (profile.getTeam() != null) {
                careerService.endSpell("İstifa");
                managerService.hireFor(profile.getTeam());
                profile.setTeam(null);
            }
        });
    }

    public ManagerProfile activeProfile() {
        return managerProfileRepository.findFirstByOrderByIdAsc()
                .filter(profile -> profile.getTeam() != null)
                .orElseThrow(() -> new ManagerModeException("\"Takımım\" modu kapalı: önce bir takım seçin"));
    }

    /**
     * Takımın sıradaki maçı: lig devam ediyorsa sıradaki haftadaki maçı; lig bittiyse kupanın oynanacak
     * turundaki maçı. Yoksa boş.
     */
    @Transactional(readOnly = true)
    public Optional<Match> nextMatch(Team team) {
        Optional<Season> season = seasonRepository.findTopByOrderBySeasonNumberDesc();
        if (season.isEmpty()) {
            return Optional.empty();
        }
        List<Match> candidates;
        if (!season.get().isFinished()) {
            Optional<Integer> week = matchRepository.findFirstUnplayedWeekNumber(season.get().getId());
            candidates = matchRepository.findLeagueMatchesBySeason(season.get().getId()).stream()
                    .filter(match -> week.isPresent() && match.getMatchWeek().getWeekNumber() == week.get())
                    .toList();
        } else {
            List<Match> cup = matchRepository.findCupMatchesBySeason(season.get().getId());
            Optional<Long> currentRound = cup.stream().filter(match -> !match.isPlayed()).findFirst()
                    .map(match -> match.getMatchWeek().getId());
            candidates = cup.stream()
                    .filter(match -> currentRound.isPresent() && match.getMatchWeek().getId().equals(currentRound.get()))
                    .toList();
        }
        return candidates.stream()
                .filter(match -> !match.isPlayed() && involves(match, team))
                .findFirst();
    }

    @Transactional(readOnly = true)
    public LineupResponse lineup() {
        Team team = activeProfile().getTeam();
        Match match = requireNextMatch(team);
        List<Player> squad = squad(team);
        return matchLineupRepository.findByMatchIdAndTeamId(match.getId(), team.getId())
                .map(lineup -> toResponse(match, lineup, true, squad))
                .orElseGet(() -> toResponse(match, suggestedLineup(match, team, squad), false, squad));
    }

    public LineupResponse saveLineup(LineupRequest request) {
        Team team = activeProfile().getTeam();
        Match match = requireNextMatch(team);
        List<Player> squad = squad(team);
        validate(request, squad);
        MatchLineup lineup = matchLineupRepository.findByMatchIdAndTeamId(match.getId(), team.getId())
                .orElseGet(() -> MatchLineup.builder().match(match).team(team).build());
        lineup.setFormation(request.formation());
        lineup.setPlayStyle(request.playStyle());
        lineup.setStarterIdList(request.starterIds());
        lineup.setCaptainId(request.captainId());
        lineup.setPenaltyTakerId(request.penaltyTakerId());
        return toResponse(match, matchLineupRepository.save(lineup), true, squad);
    }

    /** Kayıtlı kadro yoksa önerilen kadroyu kaydeder (canlı maç başlarken). */
    public MatchLineup lineupForMatch(Match match, Team team) {
        return matchLineupRepository.findByMatchIdAndTeamId(match.getId(), team.getId())
                .orElseGet(() -> matchLineupRepository.save(suggestedLineup(match, team, squad(team))));
    }

    /** Maçı canlı izlemeden oynat: kayıtlı kadro kullanılır, yoksa yapay zekâ seçer. Hafta / tur tamamen oynanır. */
    public MyTeamResponse quickPlay() {
        Team team = activeProfile().getTeam();
        Match match = requireNextMatch(team);
        if (match.getMatchWeek().getCompetition() == Competition.LEAGUE) {
            matchSimulationService.playWeek(match.getMatchWeek().getWeekNumber(), true);
        } else {
            cupService.playRound(true);
        }
        return dashboard();
    }

    Match requireNextMatch(Team team) {
        return nextMatch(team).orElseThrow(() -> new ManagerModeException("Takımın sırada oynanacak maçı yok"));
    }

    /** Yapay zekâ önerisi: takımın dizilişinde en iyi 11 (rastgelelik yok), en güçlü kaptan, en uygun penaltıcı. */
    private static MatchLineup suggestedLineup(Match match, Team team, List<Player> squad) {
        List<Player> available = squad.stream().filter(Player::isAvailable).toList();
        List<Player> starters = MatchDetailGenerator.pickStartingEleven(available, team.getFormation(), 0);
        Player captain = starters.stream().max(Comparator.comparingInt(Player::getStrength)).orElse(null);
        Player penaltyTaker = starters.stream()
                .filter(player -> player.getPosition() != Position.GOALKEEPER)
                .max(Comparator.comparingInt(player -> player.getStrength()
                        + (player.getPosition() == Position.FORWARD ? 10 : 0)))
                .orElse(null);
        MatchLineup lineup = MatchLineup.builder()
                .match(match)
                .team(team)
                .formation(team.getFormation())
                .playStyle(team.getPlayStyle())
                .captainId(captain != null ? captain.getId() : null)
                .penaltyTakerId(penaltyTaker != null ? penaltyTaker.getId() : null)
                .build();
        lineup.setStarterIdList(starters.stream().map(Player::getId).toList());
        return lineup;
    }

    /** 11 farklı, uygun (cezalı / sakat değil) oyuncu; mevki sayıları dizilişe uymalı; kaptan ve penaltıcı sahada. */
    private static void validate(LineupRequest request, List<Player> squad) {
        Map<Long, Player> byId = squad.stream().collect(Collectors.toMap(Player::getId, player -> player));
        Set<Long> unique = new HashSet<>(request.starterIds());
        if (unique.size() != MatchDetailGenerator.STARTING_PLAYERS) {
            throw new IllegalArgumentException("İlk 11'de 11 farklı oyuncu olmalı");
        }
        for (Long id : unique) {
            Player player = byId.get(id);
            if (player == null) {
                throw new IllegalArgumentException("Oyuncu takımda değil: " + id);
            }
            if (!player.isAvailable()) {
                throw new IllegalArgumentException(player.getName() + " cezalı ya da sakat, oynayamaz");
            }
        }
        for (Position position : Position.values()) {
            long count = unique.stream().filter(id -> byId.get(id).getPosition() == position).count();
            if (count != request.formation().count(position)) {
                throw new IllegalArgumentException(request.formation().label() + " dizilişi için "
                        + request.formation().count(position) + " " + positionName(position) + " gerekli (seçilen: "
                        + count + ")");
            }
        }
        if (!unique.contains(request.captainId())) {
            throw new IllegalArgumentException("Kaptan ilk 11'de olmalı");
        }
        if (!unique.contains(request.penaltyTakerId())
                || byId.get(request.penaltyTakerId()).getPosition() == Position.GOALKEEPER) {
            throw new IllegalArgumentException("Penaltıcı ilk 11'de bir saha oyuncusu olmalı");
        }
    }

    private static String positionName(Position position) {
        return switch (position) {
            case GOALKEEPER -> "kaleci";
            case DEFENDER -> "defans";
            case MIDFIELDER -> "orta saha";
            case FORWARD -> "forvet";
        };
    }

    private List<Player> squad(Team team) {
        return playerRepository.findByTeamIdAndActiveTrueOrderByShirtNumber(team.getId());
    }

    private static LineupResponse toResponse(Match match, MatchLineup lineup, boolean saved, List<Player> squad) {
        return new LineupResponse(match.getId(), saved, lineup.getFormation(), lineup.getPlayStyle(),
                lineup.starterIdList(), lineup.getCaptainId(), lineup.getPenaltyTakerId(),
                squad.stream()
                        .map(player -> new LineupResponse.SquadPlayer(player.getId(), player.getName(),
                                player.getPosition(), player.getShirtNumber(), player.getStrength(),
                                Math.round(player.effectiveStrength() * 10) / 10.0,
                                Math.round(player.form() * 100) / 100.0, player.fatigue(), player.isAvailable(),
                                player.getSuspendedMatches(), player.getInjuredMatches(), player.getInjurySeverity()))
                        .toList());
    }

    private MyTeamResponse.NextMatch nextMatchResponse(Match match, Team team) {
        boolean home = match.getHomeTeam().getId().equals(team.getId());
        return new MyTeamResponse.NextMatch(matchMapper.toMatchResponse(match), match.getMatchWeek().getCompetition(),
                match.getMatchWeek().getWeekNumber(), match.getMatchWeek().getCupRound(), home,
                matchLineupRepository.findByMatchIdAndTeamId(match.getId(), team.getId()).isPresent());
    }

    private List<MyTeamResponse.RecentResult> recentResults(Team team, Optional<Season> season) {
        if (season.isEmpty()) {
            return List.of();
        }
        List<Match> played = new ArrayList<>(matchRepository.findLeagueMatchesBySeason(season.get().getId()));
        played.addAll(matchRepository.findCupMatchesBySeason(season.get().getId()));
        List<Match> mine = played.stream().filter(match -> match.isPlayed() && involves(match, team)).toList();
        return mine.subList(Math.max(0, mine.size() - RECENT_RESULTS), mine.size()).reversed().stream()
                .map(match -> new MyTeamResponse.RecentResult(matchMapper.toMatchResponse(match),
                        match.getMatchWeek().getCompetition(), match.getMatchWeek().getWeekNumber(),
                        match.getMatchWeek().getCupRound(), result(match, team)))
                .toList();
    }

    private static String result(Match match, Team team) {
        Team winner = match.winner();
        if (winner == null) {
            return "B";
        }
        return winner.getId().equals(team.getId()) ? "G" : "M";
    }

    private String status(Team team, Optional<Season> season, Optional<Match> next) {
        if (next.isPresent()) {
            return next.get().getMatchWeek().getCompetition() == Competition.LEAGUE
                    ? "Hafta " + next.get().getMatchWeek().getWeekNumber() + " maçı seni bekliyor"
                    : "Kupa maçı seni bekliyor";
        }
        if (season.isEmpty() || season.get().isFinished() && season.get().getCupWinner() != null) {
            return "Yeni sezon henüz başlamadı: Fikstür sayfasından başlatabilirsin";
        }
        if (season.get().isFinished()) {
            boolean cupStarted = !matchRepository.findCupMatchesBySeason(season.get().getId()).isEmpty();
            return cupStarted ? "Takımın kupada oynayacağı maç yok" : "Lig bitti: kupa başlatılmadı";
        }
        return "Sırada maç yok";
    }

    private static boolean involves(Match match, Team team) {
        return match.getHomeTeam().getId().equals(team.getId()) || match.getAwayTeam().getId().equals(team.getId());
    }
}
