package com.footballleague.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.footballleague.dto.CupResponse;
import com.footballleague.dto.StandingResponse;
import com.footballleague.entity.Competition;
import com.footballleague.entity.CupRound;
import com.footballleague.entity.Match;
import com.footballleague.entity.MatchWeek;
import com.footballleague.entity.Season;
import com.footballleague.entity.Team;
import com.footballleague.exception.CupNotAvailableException;
import com.footballleague.exception.FixtureNotGeneratedException;
import com.footballleague.exception.SeasonNotFoundException;
import com.footballleague.repository.MatchRepository;
import com.footballleague.repository.MatchWeekRepository;
import com.footballleague.repository.SeasonRepository;
import com.footballleague.repository.TeamRepository;

import lombok.RequiredArgsConstructor;

/**
 * Lig bitince ilk 8 takımla tek maçlık eleme kupası: çeyrek final (1-8, 4-5, 2-7, 3-6), yarı final, final.
 * Ligde üst sıradaki takım ev sahibidir; beraberlikte penaltı atışları. Kupa maçları moral / gücü etkilemez,
 * ama kadro, kart cezası ve sakatlıklar lig maçlarıyla aynı şekilde işler.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CupService {

    static final int CUP_TEAMS = 8;
    // Kupa haftaları lig haftalarıyla karışmasın diye 100'den başlar
    static final int CUP_WEEK_OFFSET = 100;
    private static final int SHOOTOUT_KICKS = 5;
    private static final double PENALTY_SUCCESS = 0.75;

    private final SeasonRepository seasonRepository;
    private final MatchWeekRepository matchWeekRepository;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final StandingsService standingsService;
    private final MatchSimulationService matchSimulationService;
    private final MatchMapper matchMapper;
    private final RefereeService refereeService;
    private final EconomyService economyService;

    @Transactional(readOnly = true)
    public CupResponse getCup(Long seasonId) {
        Optional<Season> season = seasonId == null
                ? seasonRepository.findTopByOrderBySeasonNumberDesc()
                : Optional.of(seasonRepository.findById(seasonId).orElseThrow(() -> new SeasonNotFoundException(seasonId)));
        if (season.isEmpty()) {
            return new CupResponse(null, null, "NO_SEASON", List.of(), null, null);
        }
        return toResponse(season.get());
    }

    public CupResponse startCup() {
        Season season = latestSeason();
        if (!season.isFinished()) {
            throw new CupNotAvailableException("Kupa, lig sezonu tamamlandıktan sonra başlar");
        }
        if (!matchRepository.findCupMatchesBySeason(season.getId()).isEmpty()) {
            throw new CupNotAvailableException("Bu sezonun kupası zaten başladı");
        }
        List<StandingResponse> standings = standingsService.getStandings(season.getId());
        if (standings.size() < CUP_TEAMS) {
            throw new CupNotAvailableException("Kupa için en az " + CUP_TEAMS + " takım gerekli");
        }

        Map<Long, Team> teamById = teamRepository.findAllById(standings.stream().limit(CUP_TEAMS)
                .map(StandingResponse::teamId).toList()).stream()
                .collect(Collectors.toMap(Team::getId, team -> team));
        List<Team> seeded = standings.stream().limit(CUP_TEAMS).map(row -> teamById.get(row.teamId())).toList();
        // Eşleşme sırası, yarı finalde 1-8 / 4-5 ve 2-7 / 3-6 kazananları karşılaşacak şekilde
        int[][] pairs = {{0, 7}, {3, 4}, {1, 6}, {2, 5}};
        List<Team[]> quarterFinals = new ArrayList<>();
        for (int[] pair : pairs) {
            quarterFinals.add(new Team[] {seeded.get(pair[0]), seeded.get(pair[1])});
        }
        createRound(season, CupRound.QUARTER_FINAL, quarterFinals);
        return toResponse(season);
    }

    public CupResponse playRound() {
        return playRound(false);
    }

    /** autoManaged: yönetilen takımın kupa maçı için kadro seçilmemişse yapay zekâ seçsin (aksi hâlde 409). */
    public CupResponse playRound(boolean autoManaged) {
        Season season = latestSeason();
        List<Match> cupMatches = matchRepository.findCupMatchesBySeason(season.getId());
        if (cupMatches.isEmpty()) {
            throw new CupNotAvailableException("Kupa henüz başlamadı");
        }
        if (season.getCupWinner() != null) {
            throw new CupNotAvailableException("Kupa tamamlandı");
        }

        MatchWeek currentWeek = cupMatches.stream().filter(match -> !match.isPlayed()).findFirst()
                .orElseThrow().getMatchWeek();
        List<Match> roundMatches = cupMatches.stream()
                .filter(match -> match.getMatchWeek().getId().equals(currentWeek.getId()))
                .toList();
        matchSimulationService.requireManagedLineup(roundMatches, autoManaged);
        finishRound(season, currentWeek, roundMatches);
        return toResponse(season);
    }

    /** Kullanıcının canlı oynadığı kupa maçından sonra turun kalan maçları ve bir sonraki tur. */
    public void playRestOfRound(Match managedMatch) {
        Season season = managedMatch.getMatchWeek().getSeason();
        MatchWeek week = managedMatch.getMatchWeek();
        List<Match> roundMatches = matchRepository.findCupMatchesBySeason(season.getId()).stream()
                .filter(match -> match.getMatchWeek().getId().equals(week.getId()))
                .toList();
        finishRound(season, week, roundMatches);
    }

    /** Turun oynanmamış maçları, beraberliklerde penaltılar, ödüller; sonra sıradaki tur ya da kupa şampiyonu. */
    private void finishRound(Season season, MatchWeek currentWeek, List<Match> roundMatches) {
        matchSimulationService.simulateMatches(roundMatches.stream().filter(match -> !match.isPlayed()).toList(),
                Competition.CUP);
        for (Match match : roundMatches) {
            if (match.getHomeScore().equals(match.getAwayScore()) && match.getHomePenalties() == null) {
                int[] shootout = penaltyShootout();
                match.setHomePenalties(shootout[0]);
                match.setAwayPenalties(shootout[1]);
            }
        }

        economyService.awardCupPrizes(season, currentWeek.getCupRound(), roundMatches);
        List<Team> winners = roundMatches.stream().map(Match::winner).toList();
        CupRound next = currentWeek.getCupRound().next();
        if (next == null) {
            season.setCupWinner(winners.getFirst());
            seasonRepository.save(season);
        } else {
            Map<Long, Integer> seeds = seeds(season);
            List<Team[]> pairings = new ArrayList<>();
            for (int i = 0; i < winners.size(); i += 2) {
                Team first = winners.get(i);
                Team second = winners.get(i + 1);
                boolean firstHome = seeds.get(first.getId()) < seeds.get(second.getId());
                pairings.add(firstHome ? new Team[] {first, second} : new Team[] {second, first});
            }
            createRound(season, next, pairings);
        }
    }

    /** Kupa başlamadıysa başlatır, kalan tüm turları oynatır. */
    public CupResponse playAll() {
        return playAll(false);
    }

    public CupResponse playAll(boolean autoManaged) {
        Season season = latestSeason();
        if (matchRepository.findCupMatchesBySeason(season.getId()).isEmpty()) {
            startCup();
        }
        while (season.getCupWinner() == null) {
            playRound(autoManaged);
        }
        return toResponse(season);
    }

    /** Klasik penaltı atışları: 5'er atış (biri yetişemeyecek duruma düşünce biter), eşitlikte tek tek. */
    static int[] penaltyShootout() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int home = 0;
        int away = 0;
        int homeKicks = 0;
        int awayKicks = 0;
        while (homeKicks < SHOOTOUT_KICKS || awayKicks < SHOOTOUT_KICKS) {
            if (homeKicks == awayKicks) {
                home += random.nextDouble() < PENALTY_SUCCESS ? 1 : 0;
                homeKicks++;
            } else {
                away += random.nextDouble() < PENALTY_SUCCESS ? 1 : 0;
                awayKicks++;
            }
            if (home + (SHOOTOUT_KICKS - homeKicks) < away || away + (SHOOTOUT_KICKS - awayKicks) < home) {
                break;
            }
        }
        while (home == away) {
            home += random.nextDouble() < PENALTY_SUCCESS ? 1 : 0;
            away += random.nextDouble() < PENALTY_SUCCESS ? 1 : 0;
        }
        return new int[] {home, away};
    }

    private void createRound(Season season, CupRound round, List<Team[]> pairings) {
        MatchWeek week = matchWeekRepository.save(MatchWeek.builder()
                .season(season)
                .weekNumber(CUP_WEEK_OFFSET + round.ordinal() + 1)
                .competition(Competition.CUP)
                .cupRound(round)
                .build());
        List<Match> matches = pairings.stream()
                .map(pair -> Match.builder().matchWeek(week).homeTeam(pair[0]).awayTeam(pair[1]).build())
                .toList();
        refereeService.assign(matches);
        matchRepository.saveAll(matches);
    }

    private Season latestSeason() {
        return seasonRepository.findTopByOrderBySeasonNumberDesc().orElseThrow(FixtureNotGeneratedException::new);
    }

    /** Takım id → o sezonki lig sırası. */
    private Map<Long, Integer> seeds(Season season) {
        return standingsService.getStandings(season.getId()).stream()
                .collect(Collectors.toMap(StandingResponse::teamId, StandingResponse::rank));
    }

    private CupResponse toResponse(Season season) {
        List<Match> matches = matchRepository.findCupMatchesBySeason(season.getId());
        String status;
        if (!season.isFinished()) {
            status = "LEAGUE_IN_PROGRESS";
        } else if (matches.isEmpty()) {
            status = "NOT_STARTED";
        } else {
            status = season.getCupWinner() != null ? "FINISHED" : "IN_PROGRESS";
        }

        Map<Long, Integer> seeds = matches.isEmpty() ? Map.of() : seeds(season);
        Map<CupRound, List<Match>> byRound = new LinkedHashMap<>();
        matches.forEach(match -> byRound.computeIfAbsent(match.getMatchWeek().getCupRound(), round -> new ArrayList<>())
                .add(match));
        List<CupResponse.Round> rounds = byRound.entrySet().stream()
                .map(entry -> new CupResponse.Round(entry.getKey(),
                        entry.getValue().getFirst().getMatchWeek().getWeekNumber(),
                        entry.getValue().stream().allMatch(Match::isPlayed),
                        entry.getValue().stream()
                                .map(match -> new CupResponse.Tie(matchMapper.toMatchResponse(match),
                                        seeds.get(match.getHomeTeam().getId()), seeds.get(match.getAwayTeam().getId()),
                                        match.winner() != null ? match.winner().getId() : null))
                                .toList()))
                .toList();

        Team winner = season.getCupWinner();
        return new CupResponse(season.getId(), season.getSeasonNumber(), status, rounds,
                winner != null ? winner.getId() : null, winner != null ? winner.getName() : null);
    }
}
