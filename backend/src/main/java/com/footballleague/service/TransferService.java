package com.footballleague.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.footballleague.dto.MarketPlayerResponse;
import com.footballleague.dto.OfferRequest;
import com.footballleague.dto.OfferResponse;
import com.footballleague.dto.TransferResponse;
import com.footballleague.dto.TransferWindowResponse;
import com.footballleague.entity.InboxMessageType;
import com.footballleague.entity.Player;
import com.footballleague.entity.Position;
import com.footballleague.entity.Season;
import com.footballleague.entity.Team;
import com.footballleague.entity.Transfer;
import com.footballleague.exception.PlayerNotFoundException;
import com.footballleague.exception.TeamNotFoundException;
import com.footballleague.exception.TransferWindowClosedException;
import com.footballleague.repository.PlayerRepository;
import com.footballleague.repository.SeasonRepository;
import com.footballleague.repository.TeamRepository;
import com.footballleague.repository.TransferRepository;

import lombok.RequiredArgsConstructor;

/**
 * Transfer penceresi: lig sezonu bitince açılır, yeni sezonun fikstürü oluşturulunca kapanır. Pencere açılırken
 * serbest oyuncu havuzu yenilenir, takımlara altyapıdan genç gelir ve yapay zekâ ilk alım turunu yapar; kapanırken
 * ikinci tur. Her transfer iki takımın gücünü ilk 11 ortalamasındaki değişim kadar etkiler.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TransferService {

    static final int MIN_SQUAD = 18;
    static final int MAX_SQUAD = 30;
    // Yapay zekâ kadroyu bunun üstünde tutmaz, fazlasını serbest bırakır
    static final int AI_MAX_SQUAD = 24;
    static final double ASKING_MARKUP = 1.1;
    // Takımın en güçlü üç oyuncusu daha pahalıya satılır
    static final double KEY_PLAYER_MARKUP = 0.3;
    static final int KEY_PLAYERS = 3;
    // İstenen bedelin bu oranına kadar olan teklife karşı teklif verilir, altı reddedilir
    static final double COUNTER_RATIO = 0.85;
    // Yapay zekâ bir transfere bütçesinin en fazla bu kadarını harcar
    static final double AI_SPEND_SHARE = 0.4;
    static final int AI_MAX_PURCHASES = 2;
    static final double AI_ACTIVITY = 0.7;
    // İlk 11'deki en zayıf oyuncu takım gücünün bu kadar altındaysa yükseltme aranır
    static final int UPGRADE_GAP = 8;
    static final int UPGRADE_MIN_GAIN = 3;
    static final int NEW_FREE_AGENTS = 12;
    static final int MAX_FREE_AGENTS = 60;
    static final int MAX_ACADEMY_PLAYERS = 2;
    private static final int RECENT_TRANSFERS = 60;
    // Yapay zekânın yönetilen takımın oyuncusunu almak yerine teklif gönderme olasılığı ve teklif aralığı
    static final double OFFER_TO_MANAGER_CHANCE = 0.5;
    static final double MIN_OFFER_RATIO = 0.9;
    static final double MAX_OFFER_RATIO = 1.15;
    // Menajer oyuncusunu satışa çıkarınca teklif gönderen en fazla takım
    static final int SALE_OFFERS = 2;

    private final SeasonRepository seasonRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final TransferRepository transferRepository;
    private final EconomyService economyService;
    private final SquadGenerator squadGenerator;
    private final PlayerDevelopment playerDevelopment;
    private final InboxService inboxService;

    @Transactional(readOnly = true)
    public TransferWindowResponse window() {
        Optional<Season> latest = seasonRepository.findTopByOrderBySeasonNumberDesc();
        if (latest.isEmpty()) {
            return new TransferWindowResponse(false, null, "Transfer penceresi ilk sezon bitince açılır");
        }
        if (!latest.get().isFinished()) {
            return new TransferWindowResponse(false, null,
                    "Sezon " + latest.get().getSeasonNumber() + " devam ediyor; pencere sezon bitince açılır");
        }
        int upcoming = latest.get().getSeasonNumber() + 1;
        return new TransferWindowResponse(true, upcoming,
                "Sezon " + upcoming + " öncesi transfer penceresi açık; yeni sezon başlayınca kapanır");
    }

    /** Aktif takımların oyuncuları ve serbest oyuncular, en güçlü başta. */
    @Transactional(readOnly = true)
    public List<MarketPlayerResponse> market() {
        List<Team> teams = teamRepository.findByActiveTrue();
        Map<Long, List<Player>> squads = squads(teams);
        List<MarketPlayerResponse> market = new ArrayList<>();
        for (Team team : teams) {
            for (Player player : squads.getOrDefault(team.getId(), List.of())) {
                market.add(toMarket(player, team, askingPrice(player, squads.get(team.getId()))));
            }
        }
        playerRepository.findByActiveTrueAndTeamIsNullAndAcademyTeamIsNullOrderByStrengthDesc()
                .forEach(player -> market.add(toMarket(player, null, 0)));
        market.sort(Comparator.comparingInt(MarketPlayerResponse::strength).reversed());
        return market;
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> recentTransfers() {
        return transferRepository.findAllWithTeams().stream().limit(RECENT_TRANSFERS).map(TransferService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> playerTransfers(Long playerId) {
        return transferRepository.findByPlayerId(playerId).stream().map(TransferService::toResponse).toList();
    }

    /**
     * Teklif: istenen bedel ve üstü kabul (transfer hemen yapılır), istenenin %85'i ve üstü karşı teklif, altı ret.
     * Satan takımın kadrosu 18'in ya da mevki şablonunun altına düşecekse satış olmaz.
     */
    public OfferResponse makeOffer(OfferRequest request) {
        Season season = openWindowSeason();
        Player player = playerRepository.findById(request.playerId())
                .filter(Player::isActive)
                .orElseThrow(() -> new PlayerNotFoundException(request.playerId()));
        Team buyer = activeTeam(request.buyerTeamId());
        requireManagedBuyer(buyer);
        if (player.getTeam() == null) {
            return new OfferResponse("ACCEPTED", 0, "Serbest oyuncu imzaladı",
                    signFreeAgent(player.getId(), buyer.getId()));
        }
        Team seller = player.getTeam();
        if (seller.getId().equals(buyer.getId())) {
            throw new IllegalArgumentException("Oyuncu zaten bu takımda");
        }
        List<Player> sellerSquad = playerRepository.findByTeamIdAndActiveTrueOrderByShirtNumber(seller.getId());
        List<Player> buyerSquad = playerRepository.findByTeamIdAndActiveTrueOrderByShirtNumber(buyer.getId());
        checkBuyerSquad(buyerSquad);
        checkBudget(buyer, request.fee());
        long asking = askingPrice(player, sellerSquad);
        if (!canSell(sellerSquad, player)) {
            return new OfferResponse("REJECTED", asking,
                    seller.getName() + " bu oyuncuyu satarsa kadrosu eksik kalır", null);
        }
        if (request.fee() < asking) {
            if (request.fee() >= Math.round(asking * COUNTER_RATIO)) {
                return new OfferResponse("COUNTER", asking,
                        seller.getName() + " " + Economy.formatMoney(asking) + " istiyor", null);
            }
            return new OfferResponse("REJECTED", asking, "Teklif çok düşük bulundu", null);
        }
        Transfer transfer = move(player, buyer, request.fee(), season, buyerSquad, sellerSquad);
        return new OfferResponse("ACCEPTED", asking, player.getName() + " " + MatchCommentary.dative(buyer.getName()) + " transfer oldu",
                toResponse(transfer));
    }

    public TransferResponse signFreeAgent(Long playerId, Long teamId) {
        Season season = openWindowSeason();
        Player player = playerRepository.findById(playerId)
                .filter(found -> found.isActive() && found.getTeam() == null)
                .orElseThrow(() -> new PlayerNotFoundException(playerId));
        Team team = activeTeam(teamId);
        requireManagedBuyer(team);
        List<Player> squad = playerRepository.findByTeamIdAndActiveTrueOrderByShirtNumber(teamId);
        checkBuyerSquad(squad);
        return toResponse(move(player, team, 0, season, squad, null));
    }

    /** Menajer modunda kullanıcı yalnızca kendi takımı için transfer yapabilir. */
    private void requireManagedBuyer(Team buyer) {
        inboxService.managedTeam().filter(managed -> !managed.getId().equals(buyer.getId())).ifPresent(managed -> {
            throw new IllegalArgumentException("Menajer modunda yalnızca kendi takımın (" + managed.getName()
                    + ") için transfer yapabilirsin");
        });
    }

    /**
     * Gelen kutusundaki teklifi kabul: oyuncu yönetilen takımdan alıcıya geçer. Pencere kapalıysa, oyuncu artık
     * takımda değilse, satış kadroyu eksiltecekse ya da alıcının bütçesi yetmiyorsa hata.
     */
    public TransferResponse acceptManagerOffer(Player player, Team buyer, long fee) {
        Season season = openWindowSeason();
        Team seller = player.getTeam();
        if (seller == null || !inboxService.isManaged(seller)) {
            throw new IllegalArgumentException("Oyuncu artık takımında değil");
        }
        List<Player> sellerSquad = playerRepository.findByTeamIdAndActiveTrueOrderByShirtNumber(seller.getId());
        if (!canSell(sellerSquad, player)) {
            throw new IllegalArgumentException("Bu satış kadronu 18'in ya da mevki gereksiniminin altına düşürür");
        }
        if ((buyer.getBudget() == null ? 0 : buyer.getBudget()) < fee) {
            throw new IllegalArgumentException(buyer.getName() + " artık bu bedeli ödeyemiyor");
        }
        List<Player> buyerSquad = playerRepository.findByTeamIdAndActiveTrueOrderByShirtNumber(buyer.getId());
        checkBuyerSquad(buyerSquad);
        return toResponse(move(player, buyer, fee, season, buyerSquad, sellerSquad));
    }

    /** Menajer oyuncusunu satışa çıkarır: bedeli karşılayabilen en fazla iki takım gelen kutusuna teklif gönderir. */
    public int listForSale(Player player) {
        openWindowSeason();
        List<Player> squad = playerRepository.findByTeamIdAndActiveTrueOrderByShirtNumber(player.getTeam().getId());
        if (!canSell(squad, player)) {
            throw new IllegalArgumentException("Bu oyuncu satılırsa kadron 18'in ya da mevki gereksiniminin altına düşer");
        }
        long asking = askingPrice(player, squad);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<Team> buyers = new ArrayList<>(teamRepository.findByActiveTrue().stream()
                .filter(team -> !team.getId().equals(player.getTeam().getId()))
                .filter(team -> team.getBudget() != null && team.getBudget() * AI_SPEND_SHARE >= asking * MIN_OFFER_RATIO)
                .toList());
        Collections.shuffle(buyers, random);
        int offers = 0;
        for (Team buyer : buyers.subList(0, Math.min(SALE_OFFERS, buyers.size()))) {
            postOffer(player, buyer, Math.min(Math.round(asking * random.nextDouble(MIN_OFFER_RATIO, MAX_OFFER_RATIO)
                    / 10_000) * 10_000, Math.round(buyer.getBudget() * AI_SPEND_SHARE)));
            offers++;
        }
        return offers;
    }

    private void postOffer(Player player, Team buyer, long fee) {
        inboxService.post(InboxMessageType.TRANSFER_OFFER, buyer.getName() + " " + player.getName() + " için teklif yaptı",
                buyer.getName() + ", " + player.getName() + " için " + Economy.formatMoney(fee)
                        + " teklif ediyor. Kabul edersen oyuncu hemen ayrılır.", player, buyer, fee);
    }

    /**
     * Lig bitince: serbest oyuncular yaşlanır (emeklilik), havuza yeni serbest oyuncular eklenir, her takıma
     * altyapıdan 1-2 genç gelir, yapay zekâ ilk alım turunu yapar, eksik mevkiler gençlerle tamamlanır.
     */
    public void openWindow(Season finished) {
        Set<String> usedNames = playerRepository.findAll().stream().map(Player::getName)
                .collect(Collectors.toCollection(HashSet::new));
        refreshFreeAgents(usedNames);
        List<Team> teams = teamRepository.findByActiveTrue();
        academyIntake(teams, usedNames);
        aiRound(finished, teams);
        ensureSquads(teams);
    }

    /** Yeni sezon başlarken (fikstür oluşturulmadan önce): yapay zekânın ikinci turu ve kadro tamamlama. */
    public void closeWindow() {
        Optional<Season> latest = seasonRepository.findTopByOrderBySeasonNumberDesc().filter(Season::isFinished);
        if (latest.isEmpty()) {
            return;
        }
        List<Team> teams = teamRepository.findByActiveTrue();
        aiRound(latest.get(), teams);
        ensureSquads(teams);
    }

    private void refreshFreeAgents(Set<String> usedNames) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<Player> pool = new ArrayList<>(playerRepository.findByActiveTrueAndTeamIsNullAndAcademyTeamIsNullOrderByStrengthDesc());
        for (Player player : List.copyOf(pool)) {
            player.setAge(player.getAge() + 1);
            if (playerDevelopment.retires(player.getAge())) {
                player.setActive(false);
                pool.remove(player);
            }
        }
        List<Position> positions = templatePositions();
        List<Player> newcomers = new ArrayList<>();
        for (int i = 0; i < NEW_FREE_AGENTS; i++) {
            newcomers.add(squadGenerator.freeAgent(positions.get(random.nextInt(positions.size())), usedNames));
        }
        playerRepository.saveAll(newcomers);
        pool.addAll(newcomers);
        // Havuz çok büyürse en zayıflar futbolu bırakır
        pool.sort(Comparator.comparingInt(Player::getStrength).reversed());
        pool.stream().skip(MAX_FREE_AGENTS).forEach(player -> player.setActive(false));
    }

    private void academyIntake(List<Team> teams, Set<String> usedNames) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Map<Long, List<Player>> squads = squads(teams);
        List<Position> positions = templatePositions();
        List<Player> youth = new ArrayList<>();
        Long managedId = inboxService.managedTeam().map(Team::getId).orElse(null);
        for (Team team : teams) {
            if (team.getId().equals(managedId)) {
                academyProspects(team, usedNames, positions);
                continue;
            }
            List<Player> squad = squads.getOrDefault(team.getId(), List.of());
            Set<Integer> usedNumbers = squad.stream().map(Player::getShirtNumber).collect(Collectors.toCollection(HashSet::new));
            int count = random.nextInt(1, MAX_ACADEMY_PLAYERS + 1);
            for (int i = 0; i < count && squad.size() + i < AI_MAX_SQUAD; i++) {
                Player player = squadGenerator.academyPlayer(team, positions.get(random.nextInt(positions.size())),
                        usedNames, usedNumbers);
                usedNumbers.add(player.getShirtNumber());
                youth.add(player);
            }
        }
        economyService.signNewPlayers(youth);
        playerRepository.saveAll(youth);
    }

    /** Yönetilen takımın altyapı gençleri kadroya girmeden menajerin kararını bekler (terfi ettir / bırak). */
    private void academyProspects(Team team, Set<String> usedNames, List<Position> positions) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int count = random.nextInt(1, MAX_ACADEMY_PLAYERS + 2);
        for (int i = 0; i < count; i++) {
            Player prospect = squadGenerator.academyPlayer(team, positions.get(random.nextInt(positions.size())),
                    usedNames, Set.of());
            prospect.setTeam(null);
            prospect.setAcademyTeam(team);
            prospect.setShirtNumber(0);
            playerRepository.save(prospect);
            inboxService.post(InboxMessageType.YOUTH, "Altyapıdan yeni yetenek: " + prospect.getName(),
                    prospect.getAge() + " yaşında, güç " + prospect.getStrength() + ". Kadroya terfi ettirebilir ya da"
                            + " bırakabilirsin.", prospect, team, null);
        }
    }

    /** Her takım (rastgele sırayla, %70 olasılıkla) en acil ihtiyacı için en fazla iki oyuncu alır. */
    void aiRound(Season finished, List<Team> teams) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Map<Long, List<Player>> squads = squads(teams);
        List<Player> freeAgents = new ArrayList<>(playerRepository.findByActiveTrueAndTeamIsNullAndAcademyTeamIsNullOrderByStrengthDesc());
        // Bu pencerede zaten transfer olmuş oyuncu yapay zekâ tarafından tekrar satın alınmaz
        Set<Long> movedThisWindow = new HashSet<>(
                transferRepository.findPlayerIdsBySeasonNumber(finished.getSeasonNumber() + 1));
        Long managedId = inboxService.managedTeam().map(Team::getId).orElse(null);
        List<Team> order = new ArrayList<>(teams);
        Collections.shuffle(order, random);
        for (Team team : order) {
            // Yönetilen takımın transferlerini kullanıcı yapar
            if (team.getId().equals(managedId) || random.nextDouble() >= AI_ACTIVITY) {
                continue;
            }
            List<Player> squad = squads.computeIfAbsent(team.getId(), id -> new ArrayList<>());
            for (int i = 0; i < AI_MAX_PURCHASES && squad.size() < AI_MAX_SQUAD; i++) {
                Need need = need(team, squad);
                if (need == null) {
                    break;
                }
                Player target = bestTarget(team, need, squads, freeAgents, movedThisWindow);
                if (target == null) {
                    break;
                }
                movedThisWindow.add(target.getId());
                // Yönetilen takımın oyuncusu doğrudan alınmaz; menajere teklif gider (kabul ederse transfer olur)
                if (target.getTeam() != null && target.getTeam().getId().equals(managedId)) {
                    if (random.nextDouble() < OFFER_TO_MANAGER_CHANCE && !inboxService.hasOpenOffer(target)) {
                        postOffer(target, team, Math.round(askingPrice(target, squads.get(managedId))
                                * random.nextDouble(MIN_OFFER_RATIO, MAX_OFFER_RATIO) / 10_000) * 10_000);
                    }
                    continue;
                }
                List<Player> sellerSquad = target.getTeam() == null ? null : squads.get(target.getTeam().getId());
                long fee = sellerSquad == null ? 0 : askingPrice(target, sellerSquad);
                freeAgents.remove(target);
                move(target, team, fee, finished, squad, sellerSquad);
            }
            releaseSurplus(squad);
        }
    }

    private record Need(Position position, int minStrength) {
    }

    /** Önce mevki şablonundaki eksik, yoksa takım gücünün belirgin altında kalan en zayıf ilk 11 mevkisi. */
    private static Need need(Team team, List<Player> squad) {
        for (Map.Entry<Position, Integer> slot : SquadGenerator.SQUAD_TEMPLATE.entrySet()) {
            if (squad.stream().filter(player -> player.getPosition() == slot.getKey()).count() < slot.getValue()) {
                return new Need(slot.getKey(), 0);
            }
        }
        Need best = null;
        int bestGap = UPGRADE_GAP - 1;
        for (Position position : Position.values()) {
            Player weakest = SquadStrength.weakestStarter(squad, team.getFormation(), position);
            if (weakest != null && team.getStrength() - weakest.getStrength() > bestGap) {
                bestGap = team.getStrength() - weakest.getStrength();
                best = new Need(position, weakest.getStrength() + UPGRADE_MIN_GAIN);
            }
        }
        return best;
    }

    /** İhtiyaca uyan, satılabilir ve bütçeye uygun en güçlü oyuncu (serbest oyuncular bedelsiz). */
    private static Player bestTarget(Team buyer, Need need, Map<Long, List<Player>> squads, List<Player> freeAgents,
            Set<Long> excluded) {
        long budget = buyer.getBudget() == null ? 0 : buyer.getBudget();
        long maxFee = budget <= 0 ? 0 : Math.round(budget * AI_SPEND_SHARE);
        List<Player> candidates = new ArrayList<>();
        freeAgents.stream()
                .filter(player -> player.getPosition() == need.position() && player.getStrength() >= need.minStrength())
                .forEach(candidates::add);
        for (Map.Entry<Long, List<Player>> entry : squads.entrySet()) {
            if (entry.getKey().equals(buyer.getId())) {
                continue;
            }
            for (Player player : entry.getValue()) {
                if (player.getPosition() == need.position() && player.getStrength() >= need.minStrength()
                        && canSell(entry.getValue(), player) && askingPrice(player, entry.getValue()) <= maxFee) {
                    candidates.add(player);
                }
            }
        }
        return candidates.stream()
                .filter(player -> !excluded.contains(player.getId()))
                .max(Comparator.comparingInt(Player::getStrength)
                        .thenComparing(player -> player.getTeam() == null))
                .orElse(null);
    }

    /** Kadro AI_MAX_SQUAD'ı aşarsa en zayıf (eşitlikte en yaşlı) oyuncular, şablonu bozmadan serbest bırakılır. */
    private void releaseSurplus(List<Player> squad) {
        List<Player> byWeakness = squad.stream()
                .sorted(Comparator.comparingInt(Player::getStrength).thenComparing(Player::getAge, Comparator.reverseOrder()))
                .toList();
        for (Player player : byWeakness) {
            if (squad.size() <= AI_MAX_SQUAD) {
                return;
            }
            if (canSell(squad, player)) {
                squad.remove(player);
                release(player);
            }
        }
    }

    private static void release(Player player) {
        player.setTeam(null);
        player.setShirtNumber(0);
        player.setContractUntil(null);
        player.setWage(null);
    }

    /** Mevki şablonu (2 KL, 6 DEF, 6 OS, 4 FV) eksik kalan takımlara altyapıdan genç. */
    public void ensureSquads(List<Team> teams) {
        Map<Long, List<Player>> squads = squads(teams);
        Set<String> usedNames = playerRepository.findAll().stream().map(Player::getName)
                .collect(Collectors.toCollection(HashSet::new));
        List<Player> youth = new ArrayList<>();
        for (Team team : teams) {
            List<Player> squad = squads.getOrDefault(team.getId(), List.of());
            Set<Integer> usedNumbers = squad.stream().map(Player::getShirtNumber).collect(Collectors.toCollection(HashSet::new));
            for (Map.Entry<Position, Integer> slot : SquadGenerator.SQUAD_TEMPLATE.entrySet()) {
                long count = squad.stream().filter(player -> player.getPosition() == slot.getKey()).count();
                for (long i = count; i < slot.getValue(); i++) {
                    Player newcomer = squadGenerator.academyPlayer(team, slot.getKey(), usedNames, usedNumbers);
                    usedNumbers.add(newcomer.getShirtNumber());
                    youth.add(newcomer);
                }
            }
        }
        economyService.signNewPlayers(youth);
        playerRepository.saveAll(youth);
    }

    /**
     * Oyuncuyu alıcıya geçirir: yeni forma numarası ve sözleşme, bedel kayıtları, iki takımın gücü ilk 11
     * ortalamasındaki değişim kadar güncellenir. Listeler (kadrolar) yerinde güncellenir.
     */
    private Transfer move(Player player, Team buyer, long fee, Season season, List<Player> buyerSquad,
            List<Player> sellerSquad) {
        Team seller = player.getTeam();
        double buyerBefore = SquadStrength.topElevenAverage(buyerSquad, buyer.getFormation());
        double sellerBefore = seller == null ? 0 : SquadStrength.topElevenAverage(sellerSquad, seller.getFormation());

        if (sellerSquad != null) {
            sellerSquad.remove(player);
        }
        Set<Integer> usedNumbers = buyerSquad.stream().map(Player::getShirtNumber).collect(Collectors.toSet());
        player.setTeam(buyer);
        player.setShirtNumber(SquadGenerator.freeShirtNumber(usedNumbers, player.getPosition()));
        player.setConsecutiveStarts(0);
        economyService.signTransfer(player, season.getSeasonNumber() + 1, seller == null);
        buyerSquad.add(player);
        economyService.recordTransfer(season, seller, buyer, fee, player.getName());

        buyer.changeStrength((int) Math.round(SquadStrength.topElevenAverage(buyerSquad, buyer.getFormation()) - buyerBefore));
        if (seller != null) {
            seller.changeStrength((int) Math.round(
                    SquadStrength.topElevenAverage(sellerSquad, seller.getFormation()) - sellerBefore));
        }
        return transferRepository.save(Transfer.builder().player(player).fromTeam(seller).toTeam(buyer).fee(fee)
                .seasonNumber(season.getSeasonNumber() + 1).build());
    }

    /** Değer × 1.1; takımın en güçlü üç oyuncusundan biriyse × 1.4. 10 000'e yuvarlı. */
    static long askingPrice(Player player, List<Player> squad) {
        boolean key = squad.stream().sorted(Comparator.comparingInt(Player::getStrength).reversed())
                .limit(KEY_PLAYERS).anyMatch(candidate -> candidate == player);
        double price = Economy.marketValue(player) * (ASKING_MARKUP + (key ? KEY_PLAYER_MARKUP : 0));
        return Math.round(price / 10_000) * 10_000;
    }

    /** Satıştan sonra kadro en az 18 ve mevki şablonunda kalmalı. */
    static boolean canSell(List<Player> squad, Player player) {
        long samePosition = squad.stream().filter(candidate -> candidate.getPosition() == player.getPosition()).count();
        return squad.size() - 1 >= MIN_SQUAD
                && samePosition - 1 >= SquadGenerator.SQUAD_TEMPLATE.get(player.getPosition());
    }

    private Season openWindowSeason() {
        return seasonRepository.findTopByOrderBySeasonNumberDesc().filter(Season::isFinished)
                .orElseThrow(TransferWindowClosedException::new);
    }

    private Team activeTeam(Long id) {
        return teamRepository.findById(id).filter(Team::isActive).orElseThrow(() -> new TeamNotFoundException(id));
    }

    private static void checkBuyerSquad(List<Player> squad) {
        if (squad.size() >= MAX_SQUAD) {
            throw new IllegalArgumentException("Kadroda en fazla " + MAX_SQUAD + " oyuncu olabilir");
        }
    }

    private static void checkBudget(Team buyer, long fee) {
        long budget = buyer.getBudget() == null ? 0 : buyer.getBudget();
        if (fee > budget) {
            throw new IllegalArgumentException("Bütçe yetersiz: " + Economy.formatMoney(budget) + " var");
        }
    }

    private Map<Long, List<Player>> squads(List<Team> teams) {
        Map<Long, List<Player>> squads = new HashMap<>();
        playerRepository.findByTeamIdInAndActiveTrue(teams.stream().map(Team::getId).toList())
                .forEach(player -> squads.computeIfAbsent(player.getTeam().getId(), id -> new ArrayList<>()).add(player));
        return squads;
    }

    /** Mevki şablonundaki oranlarda rastgele mevki seçmek için (2 KL, 6 DEF, 6 OS, 4 FV). */
    private static List<Position> templatePositions() {
        List<Position> positions = new ArrayList<>();
        SquadGenerator.SQUAD_TEMPLATE.forEach((position, count) -> positions.addAll(Collections.nCopies(count, position)));
        return positions;
    }

    private static MarketPlayerResponse toMarket(Player player, Team team, long askingPrice) {
        long value = Economy.marketValue(player);
        return new MarketPlayerResponse(player.getId(), player.getName(), player.getPosition(), player.getAge(),
                player.getStrength(), Math.round(player.form() * 100) / 100.0, team != null ? team.getId() : null,
                team != null ? team.getName() : null, team == null, value, askingPrice,
                Economy.transferWage(value, team == null), player.getContractUntil());
    }

    static TransferResponse toResponse(Transfer transfer) {
        Player player = transfer.getPlayer();
        Team from = transfer.getFromTeam();
        return new TransferResponse(transfer.getId(), player.getId(), player.getName(), player.getPosition(),
                from != null ? from.getId() : null, from != null ? from.getName() : null, transfer.getToTeam().getId(),
                transfer.getToTeam().getName(), transfer.getFee(), transfer.getSeasonNumber());
    }
}
