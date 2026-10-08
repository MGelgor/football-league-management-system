package com.footballleague.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.footballleague.dto.MatchWeekResponse;
import com.footballleague.entity.Competition;
import com.footballleague.entity.InjurySeverity;
import com.footballleague.entity.ManagerProfile;
import com.footballleague.entity.Match;
import com.footballleague.entity.MatchAppearance;
import com.footballleague.entity.MatchEvent;
import com.footballleague.entity.MatchEventType;
import com.footballleague.entity.MatchLineup;
import com.footballleague.entity.MatchTeamStats;
import com.footballleague.entity.MatchWeek;
import com.footballleague.entity.Player;
import com.footballleague.entity.Season;
import com.footballleague.entity.Team;
import com.footballleague.exception.FixtureNotGeneratedException;
import com.footballleague.exception.ManagedMatchPendingException;
import com.footballleague.exception.ManagerModeException;
import com.footballleague.exception.MatchWeekNotFoundException;
import com.footballleague.exception.SeasonFinishedException;
import com.footballleague.exception.WeekAlreadyPlayedException;
import com.footballleague.exception.WeekOrderException;
import com.footballleague.repository.ManagerProfileRepository;
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
public class MatchSimulationService {

    private static final int MORALE_CHANGE = 10;
    private static final int MORALE_MIN = 0;
    private static final int MORALE_MAX = 100;
    // Güç değişimi = (alınan puan - beklenen puan) * bu katsayı, yuvarlanmış (pratikte -2..+2)
    private static final double STRENGTH_CHANGE_FACTOR = 0.7;
    // Sezonda her bu kadar sarı kartta 1 maç ceza
    static final int YELLOW_CARDS_PER_SUSPENSION = 4;
    // Uzun sakatlıkta kalıcı güç kaybı olasılığı ve miktarı
    static final double SERIOUS_INJURY_DAMAGE_CHANCE = 0.3;
    static final int MAX_SERIOUS_INJURY_DAMAGE = 3;

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
    private final TacticsService tacticsService;
    private final EconomyService economyService;
    private final MatchLineupRepository matchLineupRepository;
    private final ManagerProfileRepository managerProfileRepository;
    private final CareerService careerService;

    public MatchWeekResponse playWeek(Integer weekNumber) {
        return playWeek(weekNumber, false);
    }

    /**
     * autoManaged: kullanıcının yönettiği takımın bu haftaki maçı için kadro seçilmemişse yapay zekâ seçsin;
     * false ise kadro seçilmeden hafta oynatılamaz (409).
     */
    public MatchWeekResponse playWeek(Integer weekNumber, boolean autoManaged) {
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
        requireManagedLineup(matches, autoManaged);

        simulateMatches(matches, Competition.LEAGUE);
        finishSeasonIfDone(season);

        return new MatchWeekResponse(weekNumber, matches.stream().map(matchMapper::toMatchResponse).toList());
    }

    /** Kullanıcının canlı oynadığı maçtan sonra haftanın kalan maçları (ve gerekirse sezon sonu). */
    public void playRestOfLeagueWeek(Match managedMatch) {
        MatchWeek week = managedMatch.getMatchWeek();
        List<Match> rest = matchRepository.findByMatchWeekIdOrderById(week.getId()).stream()
                .filter(match -> !match.isPlayed())
                .toList();
        simulateMatches(rest, Competition.LEAGUE);
        finishSeasonIfDone(week.getSeason());
    }

    private void finishSeasonIfDone(Season season) {
        if (matchRepository.findFirstUnplayedWeekNumber(season.getId()).isEmpty()) {
            seasonEndService.finishSeason(season);
        }
    }

    /** Yönetilen takımın bu maçlarda kadrosu seçilmemişse (ve yapay zekâya bırakılmadıysa) 409. */
    public void requireManagedLineup(List<Match> matches, boolean autoManaged) {
        if (autoManaged) {
            return;
        }
        managerProfileRepository.findFirstByOrderByIdAsc()
                .map(ManagerProfile::getTeam)
                .ifPresent(team -> matches.stream()
                        .filter(match -> !match.isPlayed() && (match.getHomeTeam().getId().equals(team.getId())
                                || match.getAwayTeam().getId().equals(team.getId())))
                        .filter(match -> matchLineupRepository.findByMatchIdAndTeamId(match.getId(), team.getId()).isEmpty())
                        .findFirst()
                        .ifPresent(match -> {
                            throw new ManagedMatchPendingException(team.getName());
                        }));
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
        Map<Long, List<Player>> squads = loadSquads(touchedTeams);
        Map<Long, List<MatchLineup>> lineups = matchLineupRepository
                .findByMatchIdIn(matches.stream().map(Match::getId).toList()).stream()
                .collect(Collectors.groupingBy(lineup -> lineup.getMatch().getId()));

        MatchResults results = new MatchResults();
        for (Match match : matches) {
            List<Player> homeSquad = squads.getOrDefault(match.getHomeTeam().getId(), List.of());
            List<Player> awaySquad = squads.getOrDefault(match.getAwayTeam().getId(), List.of());
            List<MatchLineup> matchLineups = lineups.getOrDefault(match.getId(), List.of());

            MatchDetailGenerator.GeneratedDetail detail = simulateMatch(match, homeSquad, awaySquad,
                    competition == Competition.LEAGUE, lineupOf(matchLineups, match.getHomeTeam()),
                    lineupOf(matchLineups, match.getAwayTeam()));
            results.add(match, detail, homeSquad, awaySquad);
        }

        economyService.recordMatchFinances(matches, competition, squads);
        results.save(matches, touchedTeams);
        careerService.onMatchesPlayed(matches);
    }

    /**
     * Kullanıcının canlı (iki devre) oynadığı maçın sonucunu kaydeder. Üretici devre arasında transaction dışında
     * tutulduğu için olaylar / kadro kayıtları bu transaction'da yüklenen oyuncu ve takımlara yeniden bağlanır.
     */
    public void recordManagedResult(Long matchId, MatchDetailGenerator.GeneratedDetail detail,
            ScoreSimulator.Probabilities probabilities, ScoreSimulator.TeamSetup homeSetup,
            ScoreSimulator.TeamSetup awaySetup) {
        Match match = matchRepository.findDetailById(matchId).orElseThrow();
        if (match.isPlayed()) {
            throw new ManagerModeException("Bu maç zaten oynandı");
        }
        Team home = match.getHomeTeam();
        Team away = match.getAwayTeam();
        Map<Long, List<Player>> squads = loadSquads(List.of(home, away));
        List<Player> homeSquad = squads.getOrDefault(home.getId(), List.of());
        List<Player> awaySquad = squads.getOrDefault(away.getId(), List.of());
        rebind(detail, match, homeSquad, awaySquad);

        int homeGoals = (int) countGoals(detail.events(), home);
        int awayGoals = (int) countGoals(detail.events(), away);
        boolean league = match.getMatchWeek().getCompetition() == Competition.LEAGUE;
        applyResult(match, homeGoals, awayGoals, probabilities, league);
        setTactics(detail, homeSetup, awaySetup);

        MatchResults results = new MatchResults();
        results.add(match, detail, homeSquad, awaySquad);
        economyService.recordMatchFinances(List.of(match), match.getMatchWeek().getCompetition(), squads);
        results.save(List.of(match), List.of(home, away));
        careerService.onMatchesPlayed(List.of(match));
    }

    private Map<Long, List<Player>> loadSquads(Collection<Team> teams) {
        return playerRepository.findByTeamIdInAndActiveTrue(teams.stream().map(Team::getId).toList()).stream()
                .collect(Collectors.groupingBy(player -> player.getTeam().getId()));
    }

    private static MatchLineup lineupOf(List<MatchLineup> lineups, Team team) {
        return lineups.stream().filter(lineup -> lineup.getTeam().getId().equals(team.getId())).findFirst().orElse(null);
    }

    /** Oynanan maçların olay / kadro / istatistik kayıtları ve oyuncu durum güncellemeleri. */
    private final class MatchResults {

        private final List<MatchEvent> events = new ArrayList<>();
        private final List<MatchAppearance> appearances = new ArrayList<>();
        private final List<MatchTeamStats> stats = new ArrayList<>();

        void add(Match match, MatchDetailGenerator.GeneratedDetail detail, List<Player> homeSquad,
                List<Player> awaySquad) {
            updatePlayerStatuses(homeSquad, detail.events());
            updatePlayerStatuses(awaySquad, detail.events());
            updateFormAndFatigue(homeSquad, detail.appearances());
            updateFormAndFatigue(awaySquad, detail.appearances());
            detail.homeStats().setStrengthAfter(match.getHomeTeam().getStrength());
            detail.awayStats().setStrengthAfter(match.getAwayTeam().getStrength());
            events.addAll(detail.events());
            appearances.addAll(detail.appearances());
            stats.add(detail.homeStats());
            stats.add(detail.awayStats());
        }

        void save(List<Match> matches, Collection<Team> teams) {
            matchRepository.saveAll(matches);
            matchEventRepository.saveAll(events);
            matchAppearanceRepository.saveAll(appearances);
            matchTeamStatsRepository.saveAll(stats);
            teamRepository.saveAll(teams);
        }
    }

    private MatchDetailGenerator.GeneratedDetail simulateMatch(Match match, List<Player> homeSquad,
            List<Player> awaySquad, boolean league, MatchLineup homeLineup, MatchLineup awayLineup) {
        Team home = match.getHomeTeam();
        Team away = match.getAwayTeam();
        List<Player> homeAvailable = homeSquad.stream().filter(Player::isAvailable).toList();
        List<Player> awayAvailable = awaySquad.stream().filter(Player::isAvailable).toList();

        ScoreSimulator.TeamSetup homeSetup = tacticsService.setup(home, away, true, homeLineup);
        ScoreSimulator.TeamSetup awaySetup = tacticsService.setup(away, home, false, awayLineup);
        MatchDetailGenerator.SideSetup homeSide = sideSetup(homeAvailable, homeSetup, homeLineup, true);
        MatchDetailGenerator.SideSetup awaySide = sideSetup(awayAvailable, awaySetup, awayLineup, true);
        ScoreSimulator.ExpectedGoals expected = withLineupQuality(scoreSimulator.expectedGoals(homeSetup, awaySetup),
                homeSide, awaySide);
        ScoreSimulator.Probabilities probabilities = scoreSimulator.probabilities(expected);
        ScoreSimulator.SimulatedScore score = scoreSimulator.simulate(expected);

        match.setHomeScore(score.homeGoals());
        match.setAwayScore(score.awayGoals());
        applyResult(match, score.homeGoals(), score.awayGoals(), probabilities, league);

        MatchDetailGenerator.GeneratedDetail detail = matchDetailGenerator.generate(match, homeSide, awaySide, expected);
        setTactics(detail, homeSetup, awaySetup);
        return detail;
    }

    /**
     * Kullanıcının seçtiği kadro varsa (oyuncuları hâlâ uygunsa) o ilk 11; yoksa yapay zekâ seçer.
     * autoSubstitutions false: değişiklikleri kullanıcı yapar (canlı maç).
     */
    public static MatchDetailGenerator.SideSetup sideSetup(List<Player> available, ScoreSimulator.TeamSetup setup,
            MatchLineup lineup, boolean autoSubstitutions) {
        if (lineup == null) {
            return MatchDetailGenerator.SideSetup.auto(available, setup.formation());
        }
        Map<Long, Player> byId = available.stream().collect(Collectors.toMap(Player::getId, player -> player));
        List<Player> starters = lineup.starterIdList().stream().map(byId::get).filter(Objects::nonNull).toList();
        if (starters.size() != MatchDetailGenerator.STARTING_PLAYERS) {
            return MatchDetailGenerator.SideSetup.auto(available, setup.formation());
        }
        Player penaltyTaker = lineup.getPenaltyTakerId() == null ? null : byId.get(lineup.getPenaltyTakerId());
        return new MatchDetailGenerator.SideSetup(available, setup.formation(), starters, penaltyTaker,
                autoSubstitutions);
    }

    /** Kullanıcı ilk 11'i en iyi 11'den zayıfsa gol beklentisi düşer (TacticsService.lineupQuality). */
    public static ScoreSimulator.ExpectedGoals withLineupQuality(ScoreSimulator.ExpectedGoals expected,
            MatchDetailGenerator.SideSetup home, MatchDetailGenerator.SideSetup away) {
        return new ScoreSimulator.ExpectedGoals(expected.home() * TacticsService.lineupQuality(home),
                expected.away() * TacticsService.lineupQuality(away));
    }

    private void applyResult(Match match, int homeGoals, int awayGoals, ScoreSimulator.Probabilities probabilities,
            boolean league) {
        Team home = match.getHomeTeam();
        Team away = match.getAwayTeam();
        match.setHomeScore(homeGoals);
        match.setAwayScore(awayGoals);
        match.setHomeWinProbability(probabilities.homeWinPercent());
        match.setDrawProbability(probabilities.drawPercent());
        match.setAwayWinProbability(probabilities.awayWinPercent());
        if (league) {
            applyMoraleChange(home, away, homeGoals, awayGoals);
            home.changeStrength(strengthChange(points(homeGoals, awayGoals), probabilities.homeExpectedPoints()));
            away.changeStrength(strengthChange(points(awayGoals, homeGoals), probabilities.awayExpectedPoints()));
        }
    }

    private static void setTactics(MatchDetailGenerator.GeneratedDetail detail, ScoreSimulator.TeamSetup homeSetup,
            ScoreSimulator.TeamSetup awaySetup) {
        detail.homeStats().setFormation(homeSetup.formation());
        detail.homeStats().setPlayStyle(homeSetup.style());
        detail.awayStats().setFormation(awaySetup.formation());
        detail.awayStats().setPlayStyle(awaySetup.style());
    }

    /** Takımın skoruna yazılan goller: kendi golleri + rakibin kendi kalesine golleri. */
    private static long countGoals(List<MatchEvent> events, Team team) {
        return events.stream().filter(event -> event.getType() == MatchEventType.GOAL
                        ? event.getTeam().getId().equals(team.getId())
                        : event.getType() == MatchEventType.OWN_GOAL && !event.getTeam().getId().equals(team.getId()))
                .count();
    }

    /** Olay / kadro / istatistik kayıtlarındaki maç, takım ve oyuncu referanslarını yüklenmiş nesnelerle değiştirir. */
    private static void rebind(MatchDetailGenerator.GeneratedDetail detail, Match match, List<Player> homeSquad,
            List<Player> awaySquad) {
        Map<Long, Player> players = new HashMap<>();
        homeSquad.forEach(player -> players.put(player.getId(), player));
        awaySquad.forEach(player -> players.put(player.getId(), player));
        Function<Team, Team> team = detached -> detached.getId().equals(match.getHomeTeam().getId())
                ? match.getHomeTeam() : match.getAwayTeam();
        for (MatchEvent event : detail.events()) {
            event.setMatch(match);
            event.setTeam(team.apply(event.getTeam()));
            event.setPlayer(players.get(event.getPlayer().getId()));
            if (event.getAssistPlayer() != null) {
                event.setAssistPlayer(players.get(event.getAssistPlayer().getId()));
            }
        }
        for (MatchAppearance appearance : detail.appearances()) {
            appearance.setMatch(match);
            appearance.setTeam(team.apply(appearance.getTeam()));
            appearance.setPlayer(players.get(appearance.getPlayer().getId()));
            if (appearance.getReplacedPlayer() != null) {
                appearance.setReplacedPlayer(players.get(appearance.getReplacedPlayer().getId()));
            }
        }
        for (MatchTeamStats stats : List.of(detail.homeStats(), detail.awayStats())) {
            stats.setMatch(match);
            stats.setTeam(team.apply(stats.getTeam()));
        }
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
                if (player.getInjuredMatches() == 0) {
                    player.setInjurySeverity(null);
                }
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
                case INJURY -> injure(player, event);
                case GOAL, OWN_GOAL, PENALTY_MISSED, VAR_DISALLOWED -> {
                }
            }
        }
    }

    /** Hafif (%70) 1-2, orta (%22) 3-6, uzun (%8) 8-20 maç; uzun sakatlık kalıcı güç kaybettirebilir. */
    private static void injure(Player player, MatchEvent event) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int matches = injuryDuration(random.nextDouble());
        event.setInjuryMatches(matches);
        player.setInjuredMatches(matches);
        player.setInjurySeverity(InjurySeverity.of(matches));
        if (InjurySeverity.of(matches) == InjurySeverity.SERIOUS && random.nextDouble() < SERIOUS_INJURY_DAMAGE_CHANCE) {
            player.changeStrength(-random.nextInt(1, MAX_SERIOUS_INJURY_DAMAGE + 1));
        }
    }

    static int injuryDuration(double roll) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (roll < 0.70) {
            return random.nextInt(1, 3);
        }
        return roll < 0.92 ? random.nextInt(3, 7) : random.nextInt(8, 21);
    }

    /** Oynayanların reytingi forma eklenir; ilk 11'de başlayanın üst üste maç sayısı artar, diğerlerinin sıfırlanır. */
    static void updateFormAndFatigue(List<Player> squad, List<MatchAppearance> appearances) {
        for (Player player : squad) {
            MatchAppearance appearance = appearances.stream()
                    .filter(candidate -> candidate.getPlayer() == player)
                    .findFirst()
                    .orElse(null);
            if (appearance != null) {
                player.addRating(appearance.getRating());
            }
            player.setConsecutiveStarts(appearance != null && appearance.isStarter()
                    ? player.getConsecutiveStarts() + 1 : 0);
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
