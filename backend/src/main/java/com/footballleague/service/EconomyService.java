package com.footballleague.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.footballleague.dto.FinanceResponse;
import com.footballleague.dto.StandingResponse;
import com.footballleague.entity.Competition;
import com.footballleague.entity.CupRound;
import com.footballleague.entity.FinanceEntry;
import com.footballleague.entity.FinanceType;
import com.footballleague.entity.InboxMessageType;
import com.footballleague.entity.Match;
import com.footballleague.entity.MatchWeek;
import com.footballleague.entity.Player;
import com.footballleague.entity.Season;
import com.footballleague.entity.Team;
import com.footballleague.exception.SeasonNotFoundException;
import com.footballleague.exception.TeamNotFoundException;
import com.footballleague.repository.FinanceEntryRepository;
import com.footballleague.repository.PlayerRepository;
import com.footballleague.repository.SeasonRepository;
import com.footballleague.repository.TeamRepository;

import lombok.RequiredArgsConstructor;

/**
 * Bütçe ve sözleşmeler. Her gelir / gider bir FinanceEntry olarak yazılır ve takımın bütçesine eklenir:
 * iç saha maçı bilet geliri, lig haftası başına maaşlar, sezon sonu lig ödülü, kupa ödülleri.
 * Sözleşmesi biten oyuncu yapay zekâ kararıyla uzatılır ya da serbest bırakılır.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class EconomyService {

    // Sözleşmeler 1-4 sezon
    private static final int MAX_EXTRA_CONTRACT_SEASONS = 3;
    private static final int RENEW_MAX_AGE = 31;
    // Takım ortalamasından bu kadar zayıf oyuncunun sözleşmesi uzatılmaz
    private static final int RENEW_STRENGTH_GAP = 10;
    private static final double RENEW_CHANCE = 0.85;
    private static final int RECENT_ENTRIES = 30;
    static final Map<CupRound, Long> CUP_ELIMINATION_PRIZE = Map.of(
            CupRound.QUARTER_FINAL, 750_000L,
            CupRound.SEMI_FINAL, 1_500_000L,
            CupRound.FINAL, 3_000_000L);
    static final long CUP_WINNER_PRIZE = 6_000_000;

    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final SeasonRepository seasonRepository;
    private final FinanceEntryRepository financeEntryRepository;
    private final InboxService inboxService;

    /** Bütçesi / sözleşmesi olmayan takım ve oyunculara (eski kayıtlar, yeni üretilen oyuncular) başlangıç değerleri. */
    public void ensureEconomy() {
        teamRepository.findByActiveTrue().stream()
                .filter(team -> team.getBudget() == null)
                .forEach(team -> team.setBudget(Economy.initialBudget(team)));
        int baseSeason = contractBaseSeason();
        playerRepository.findByActiveTrueAndTeamIsNotNullAndContractUntilIsNull()
                .forEach(player -> signContract(player, baseSeason));
    }

    /**
     * Menajerin sözleşme uzatma teklifi: oyuncu güncel değerine göre maaş ister (23 yaş altı %10, takımın en iyi
     * üç oyuncusu %15 fazla). İsteğin %95'i ve üstü kabul, %80'i ve üstü karşı istek, altı ret. seasons: 1-4.
     */
    public RenewalResult renew(Player player, long offeredWage, int seasons, boolean keyPlayer) {
        if (seasons < 1 || seasons > MAX_EXTRA_CONTRACT_SEASONS + 1) {
            throw new IllegalArgumentException("Sözleşme 1-4 sezon olabilir");
        }
        double demandFactor = (player.getAge() < 23 ? 1.1 : 1.0) * (keyPlayer ? 1.15 : 1.0);
        long demand = Math.round(Economy.weeklyWage(Economy.marketValue(player)) * demandFactor / 100) * 100;
        if (offeredWage >= demand * 0.95) {
            player.setWage(offeredWage);
            player.setContractUntil(contractBaseSeason() + seasons - 1);
            return new RenewalResult("ACCEPTED", demand, player.getName() + " sözleşmeyi imzaladı (Sezon "
                    + player.getContractUntil() + " sonuna kadar)");
        }
        if (offeredWage >= demand * 0.8) {
            return new RenewalResult("COUNTER", demand, player.getName() + " haftalık " + Economy.formatMoney(demand)
                    + " istiyor");
        }
        return new RenewalResult("REJECTED", demand, player.getName() + " teklifi düşük buldu (haftalık "
                + Economy.formatMoney(demand) + " civarı bekliyor)");
    }

    public record RenewalResult(String status, long demand, String message) {
    }

    /** Yeni oluşturulan oyunculara (takım kurulurken, elle eklenirken) sözleşme ve maaş. */
    public void signNewPlayers(List<Player> players) {
        int baseSeason = contractBaseSeason();
        players.forEach(player -> signContract(player, baseSeason));
    }

    /** Sözleşmeler bu sezonun sonuna göre sayılır: devam eden sezon, yoksa sıradaki sezon. */
    int contractBaseSeason() {
        return seasonRepository.findTopByOrderBySeasonNumberDesc()
                .map(season -> season.isFinished() ? season.getSeasonNumber() + 1 : season.getSeasonNumber())
                .orElse(1);
    }

    private static void signContract(Player player, int baseSeason) {
        player.setContractUntil(baseSeason + ThreadLocalRandom.current().nextInt(0, MAX_EXTRA_CONTRACT_SEASONS + 1));
        player.setWage(Economy.weeklyWage(Economy.marketValue(player)));
    }

    /** Transferle gelen oyuncuya 2-4 sezonluk sözleşme; firstSeason: transferin geçerli olduğu sezon. */
    public void signTransfer(Player player, int firstSeason, boolean freeAgent) {
        player.setContractUntil(firstSeason + ThreadLocalRandom.current().nextInt(1, MAX_EXTRA_CONTRACT_SEASONS + 1));
        player.setWage(Economy.transferWage(Economy.marketValue(player), freeAgent));
    }

    /** Bedelli transferde alıcıya gider, satıcıya gelir kaydı (serbest oyuncuda kayıt yok). */
    public void recordTransfer(Season season, Team seller, Team buyer, long fee, String playerName) {
        if (fee == 0 || seller == null) {
            return;
        }
        financeEntryRepository.saveAll(List.of(
                entry(buyer, season, null, FinanceType.TRANSFER_PURCHASE, -fee, playerName + " (" + seller.getName() + ")"),
                entry(seller, season, null, FinanceType.TRANSFER_SALE, fee, playerName + " (" + buyer.getName() + ")")));
    }

    /** Oynanan maçların bilet gelirleri (ev sahibine) ve lig haftasıysa iki takımın maaşları. */
    public void recordMatchFinances(List<Match> matches, Competition competition, Map<Long, List<Player>> squads) {
        List<FinanceEntry> entries = new ArrayList<>();
        for (Match match : matches) {
            MatchWeek week = match.getMatchWeek();
            Team home = match.getHomeTeam();
            Team away = match.getAwayTeam();
            entries.add(entry(home, week.getSeason(), week.getWeekNumber(), FinanceType.TICKETS,
                    Economy.ticketIncome(home), "İç saha: " + away.getName()));
            if (competition == Competition.LEAGUE) {
                for (Team team : List.of(home, away)) {
                    long wages = squads.getOrDefault(team.getId(), List.of()).stream()
                            .mapToLong(player -> player.getWage() == null ? 0 : player.getWage()).sum();
                    entries.add(entry(team, week.getSeason(), week.getWeekNumber(), FinanceType.WAGES, -wages,
                            "Hafta " + week.getWeekNumber() + " maaşları"));
                }
            }
        }
        financeEntryRepository.saveAll(entries);
    }

    public void awardLeaguePrizes(Season season, List<StandingResponse> standings, Map<Long, Team> teamById) {
        financeEntryRepository.saveAll(standings.stream()
                .map(row -> entry(teamById.get(row.teamId()), season, null, FinanceType.LEAGUE_PRIZE,
                        Economy.leaguePrize(row.rank(), standings.size()), "Lig " + row.rank() + ".'lik ödülü"))
                .toList());
    }

    /** Kupa turunda elenen takım o turun ödülünü, finali kazanan şampiyonluk ödülünü alır. */
    public void awardCupPrizes(Season season, CupRound round, List<Match> roundMatches) {
        List<FinanceEntry> entries = new ArrayList<>();
        for (Match match : roundMatches) {
            Team winner = match.winner();
            Team loser = winner == match.getHomeTeam() ? match.getAwayTeam() : match.getHomeTeam();
            entries.add(entry(loser, season, match.getMatchWeek().getWeekNumber(), FinanceType.CUP_PRIZE,
                    CUP_ELIMINATION_PRIZE.get(round), "Kupa: " + roundName(round)));
            if (round == CupRound.FINAL) {
                entries.add(entry(winner, season, match.getMatchWeek().getWeekNumber(), FinanceType.CUP_PRIZE,
                        CUP_WINNER_PRIZE, "Kupa şampiyonluğu"));
            }
        }
        financeEntryRepository.saveAll(entries);
    }

    /**
     * Sezon sonu: sözleşmesi biten oyuncular. Genç ve takım seviyesindeki oyuncunun sözleşmesi (%85) güncel
     * değerine göre maaşla 1-4 sezon uzatılır, diğerleri serbest kalır (forma numarası boşalır). Eksilen mevkiler
     * transfer penceresinde doldurulur (TransferService).
     */
    public List<Player> processContracts(Season season, List<Team> teams) {
        Map<Long, List<Player>> squads = playerRepository
                .findByTeamIdInAndActiveTrue(teams.stream().map(Team::getId).toList()).stream()
                .collect(Collectors.groupingBy(player -> player.getTeam().getId()));
        List<Player> released = new ArrayList<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (Team team : teams) {
            List<Player> squad = new ArrayList<>(squads.getOrDefault(team.getId(), List.of()));
            double average = squad.stream().mapToInt(Player::getStrength).average().orElse(0);
            boolean managed = inboxService.isManaged(team);
            for (Player player : List.copyOf(squad)) {
                if (player.getContractUntil() == null || player.getContractUntil() > season.getSeasonNumber()) {
                    continue;
                }
                // Yönetilen takımda sözleşmeyi menajer uzatır; uzatılmadıysa oyuncu ayrılır
                boolean wanted = !managed && player.getAge() <= RENEW_MAX_AGE
                        && player.getStrength() >= average - RENEW_STRENGTH_GAP && random.nextDouble() < RENEW_CHANCE;
                if (wanted) {
                    signContract(player, season.getSeasonNumber() + 1);
                } else {
                    player.setTeam(null);
                    player.setShirtNumber(0);
                    player.setContractUntil(null);
                    player.setWage(null);
                    released.add(player);
                    if (managed) {
                        inboxService.post(InboxMessageType.CONTRACT, player.getName() + " takımdan ayrıldı",
                                "Sözleşmesi uzatılmadığı için serbest kaldı.", player, team, null);
                    }
                }
            }
        }
        return released;
    }

    /** Sezon iptalinde o sezonun tüm gelir / giderleri geri alınır. */
    public void revertSeason(Season season) {
        List<FinanceEntry> entries = financeEntryRepository.findBySeasonIdWithTeam(season.getId());
        entries.forEach(entry -> entry.getTeam().addToBudget(-entry.getAmount()));
        // Kayıtlar bu transaction'da yüklendiği için toplu JPQL silme yerine entity olarak silinir
        financeEntryRepository.deleteAll(entries);
    }

    /** seasonId null ise güncel sezon. */
    @Transactional(readOnly = true)
    public FinanceResponse getFinances(Long teamId, Long seasonId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new TeamNotFoundException(teamId));
        Optional<Season> season = seasonId == null
                ? seasonRepository.findTopByOrderBySeasonNumberDesc()
                : Optional.of(seasonRepository.findById(seasonId).orElseThrow(() -> new SeasonNotFoundException(seasonId)));
        List<FinanceEntry> entries = season
                .map(s -> financeEntryRepository.findByTeamIdAndSeasonIdOrderByIdDesc(teamId, s.getId()))
                .orElse(List.of());

        Map<FinanceType, Long> byType = new EnumMap<>(FinanceType.class);
        entries.forEach(entry -> byType.merge(entry.getType(), entry.getAmount(), Long::sum));
        List<Player> squad = playerRepository.findByTeamIdAndActiveTrueOrderByShirtNumber(teamId);

        return new FinanceResponse(
                teamId,
                team.getBudget() == null ? 0 : team.getBudget(),
                season.map(Season::getSeasonNumber).orElse(null),
                entries.stream().mapToLong(FinanceEntry::getAmount).filter(amount -> amount > 0).sum(),
                -entries.stream().mapToLong(FinanceEntry::getAmount).filter(amount -> amount < 0).sum(),
                byType.entrySet().stream().map(e -> new FinanceResponse.TypeTotal(e.getKey(), e.getValue())).toList(),
                squad.stream().mapToLong(player -> player.getWage() == null ? 0 : player.getWage()).sum(),
                squad.stream().mapToLong(player -> Economy.marketValue(player)).sum(),
                squad.stream()
                        .sorted(Comparator.comparing((Player player) -> player.getContractUntil() == null
                                ? Integer.MAX_VALUE : player.getContractUntil())
                                .thenComparing(Comparator.comparingLong((Player player) -> Economy.marketValue(player)).reversed()))
                        .map(player -> new FinanceResponse.ContractLine(player.getId(), player.getName(),
                                player.getPosition(), player.getAge(), player.getStrength(), Economy.marketValue(player),
                                player.getWage() == null ? 0 : player.getWage(), player.getContractUntil()))
                        .toList(),
                entries.stream().limit(RECENT_ENTRIES)
                        .map(entry -> new FinanceResponse.EntryLine(entry.getWeekNumber(), entry.getType(),
                                entry.getAmount(), entry.getDescription()))
                        .toList());
    }

    private static FinanceEntry entry(Team team, Season season, Integer weekNumber, FinanceType type, long amount,
            String description) {
        team.addToBudget(amount);
        return FinanceEntry.builder().team(team).season(season).weekNumber(weekNumber).type(type).amount(amount)
                .description(description).build();
    }

    private static String roundName(CupRound round) {
        return switch (round) {
            case QUARTER_FINAL -> "çeyrek final";
            case SEMI_FINAL -> "yarı final";
            case FINAL -> "final";
        };
    }
}
