package com.footballleague.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.footballleague.dto.ActionResponse;
import com.footballleague.dto.CareerResponse;
import com.footballleague.dto.ContractOfferRequest;
import com.footballleague.dto.StandingResponse;
import com.footballleague.entity.Competition;
import com.footballleague.entity.CupRound;
import com.footballleague.entity.InboxMessage;
import com.footballleague.entity.InboxMessageType;
import com.footballleague.entity.ManagerProfile;
import com.footballleague.entity.ManagerSpell;
import com.footballleague.entity.Match;
import com.footballleague.entity.MatchEvent;
import com.footballleague.entity.MatchEventType;
import com.footballleague.entity.Player;
import com.footballleague.entity.Season;
import com.footballleague.entity.Team;
import com.footballleague.exception.ManagerModeException;
import com.footballleague.exception.PlayerNotFoundException;
import com.footballleague.repository.InboxMessageRepository;
import com.footballleague.repository.ManagerProfileRepository;
import com.footballleague.repository.ManagerSpellRepository;
import com.footballleague.repository.MatchEventRepository;
import com.footballleague.repository.MatchRepository;
import com.footballleague.repository.PlayerRepository;
import com.footballleague.repository.SeasonRepository;
import com.footballleague.repository.TeamRepository;

import lombok.RequiredArgsConstructor;

/**
 * Menajer kariyeri: yönetim kurulu hedefleri ve güven puanı, kovulma ve iş teklifleri, gelen kutusundaki
 * tekliflerin kabul / reddi, sözleşme pazarlığı, altyapı kararları ve takım takım kariyer özeti.
 * Güven: lig maçında galibiyet +2 / mağlubiyet −2; sezon sonunda hedef sıraya göre ±5 / sıra (en fazla +20 / −30),
 * şampiyonluk +15, eksi bütçe −15, kupa hedefi tutarsa +5 tutmazsa −10. 0'a inerse ya da takım küme düşerse kovulur.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CareerService {

    static final int START_CONFIDENCE = 60;
    static final int MAX_CONFIDENCE = 100;
    static final int MATCH_CONFIDENCE = 2;
    static final int RANK_CONFIDENCE = 5;
    static final int MAX_RANK_BONUS = 20;
    static final int MAX_RANK_PENALTY = 30;
    static final int CHAMPION_BONUS = 15;
    static final int NEGATIVE_BUDGET_PENALTY = 15;
    static final int CUP_TARGET_MET = 5;
    static final int CUP_TARGET_MISSED = 10;
    static final int JOB_OFFERS = 3;
    // İş teklifi gelen takımlar en fazla bu kadar güçlü olabilir (kovulan takımın gücüne göre)
    static final int JOB_STRENGTH_MARGIN = 5;
    private static final int RELEGATION_SPOTS = 3;

    private final ManagerProfileRepository managerProfileRepository;
    private final ManagerSpellRepository managerSpellRepository;
    private final InboxMessageRepository inboxMessageRepository;
    private final InboxService inboxService;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final SeasonRepository seasonRepository;
    private final MatchRepository matchRepository;
    private final MatchEventRepository matchEventRepository;
    private final ManagerService managerService;
    private final TransferService transferService;
    private final EconomyService economyService;

    // ---- Kariyer dönemleri

    /** Menajer bir takımı devraldı (mod açıldı, iş teklifi kabul edildi). */
    public void startSpell(ManagerProfile profile, Team team) {
        managerSpellRepository.findFirstByEndSeasonIsNullOrderByIdDesc()
                .ifPresent(spell -> closeSpell(spell, "Yeni takım"));
        // Sezon bitmişse görev sıradaki sezondan başlar
        int season = seasonRepository.findTopByOrderBySeasonNumberDesc()
                .map(found -> found.isFinished() ? found.getSeasonNumber() + 1 : found.getSeasonNumber())
                .orElse(1);
        managerSpellRepository.save(ManagerSpell.builder().team(team).startSeason(season).build());
        profile.setConfidence(START_CONFIDENCE);
        profile.setObjectiveSeason(null);
        seasonRepository.findTopByOrderBySeasonNumberDesc().filter(found -> !found.isFinished())
                .ifPresent(found -> setObjectives(profile, team, found));
        inboxService.post(InboxMessageType.WELCOME, MatchCommentary.dative(team.getName()) + " hoş geldin",
                "Yönetim kurulu sana güveniyor (güven " + START_CONFIDENCE + "/100). Hedefler sezon başında belirlenir;"
                        + " güven 0'a düşerse ya da takım küme düşerse görevine son verilir.", null, team, null);
    }

    /** Menajer takımdan ayrıldı (mod kapandı ya da kovuldu). */
    public void endSpell(String reason) {
        managerSpellRepository.findFirstByEndSeasonIsNullOrderByIdDesc().ifPresent(spell -> closeSpell(spell, reason));
    }

    private void closeSpell(ManagerSpell spell, String reason) {
        spell.setEndSeason(currentSeasonNumber());
        spell.setEndReason(reason);
    }

    // ---- Lig ve sezon olayları

    /**
     * Yeni sezon: önceki sezonun kupa hedefi değerlendirilir, yeni hedefler (lig sırası, kupa turu) konur,
     * bu sezon sonunda sözleşmesi bitecek oyuncular hatırlatılır.
     */
    public void onSeasonStart(Season season) {
        Optional<ManagerProfile> profile = managedProfile();
        if (profile.isEmpty()) {
            return;
        }
        ManagerProfile manager = profile.get();
        Team team = manager.getTeam();
        if (manager.getObjectiveSeason() != null && manager.getCupTarget() != null) {
            evaluateCup(manager, team, manager.getObjectiveSeason());
            if (manager.getConfidence() <= 0) {
                sack(manager, "Güven kalmadı", List.of());
                return;
            }
        }
        setObjectives(manager, team, season);
        List<Player> expiring = playerRepository.findByTeamIdAndActiveTrueOrderByShirtNumber(team.getId()).stream()
                .filter(player -> player.getContractUntil() != null && player.getContractUntil() <= season.getSeasonNumber())
                .toList();
        if (!expiring.isEmpty()) {
            inboxService.post(InboxMessageType.CONTRACT, expiring.size() + " oyuncunun sözleşmesi bu sezon bitiyor",
                    String.join(", ", expiring.stream().map(Player::getName).toList())
                            + ". Uzatmazsan sezon sonunda serbest kalırlar.", null, team, null);
        }
    }

    /** Oynanan maçlar: yönetilen takımın sonucu güveni değiştirir, sakatlıklar gelen kutusuna düşer. */
    public void onMatchesPlayed(List<Match> matches) {
        Optional<ManagerProfile> profile = managedProfile();
        if (profile.isEmpty()) {
            return;
        }
        Team team = profile.get().getTeam();
        for (Match match : matches) {
            boolean home = match.getHomeTeam().getId().equals(team.getId());
            if (!match.isPlayed() || (!home && !match.getAwayTeam().getId().equals(team.getId()))) {
                continue;
            }
            Team winner = match.winner();
            if (match.getMatchWeek().getCompetition() == Competition.LEAGUE && winner != null) {
                adjust(profile.get(), winner.getId().equals(team.getId()) ? MATCH_CONFIDENCE : -MATCH_CONFIDENCE);
            }
            for (MatchEvent event : matchEventRepository.findByMatchIdWithPlayers(match.getId())) {
                if (event.getType() == MatchEventType.INJURY && event.getTeam().getId().equals(team.getId())) {
                    Integer matchesOut = event.getPlayer().getInjuredMatches();
                    inboxService.post(InboxMessageType.INJURY, event.getPlayer().getName() + " sakatlandı",
                            event.getMinute() + ". dakikada sakatlandı; " + matchesOut + " maç forma giyemeyecek.",
                            event.getPlayer(), team, null);
                }
            }
        }
    }

    /** Lig bitti: hedef sıra, şampiyonluk ve bütçeye göre güven; küme düşen ya da güveni biten menajer kovulur. */
    public void onLeagueFinished(Season season, List<StandingResponse> standings, List<Long> relegatedIds) {
        Optional<ManagerProfile> profile = managedProfile();
        if (profile.isEmpty()) {
            return;
        }
        ManagerProfile manager = profile.get();
        Team team = manager.getTeam();
        Optional<StandingResponse> row = standings.stream().filter(r -> r.teamId().equals(team.getId())).findFirst();
        if (row.isEmpty()) {
            return;
        }
        int rank = row.get().rank();
        int target = manager.getTargetRank() != null ? manager.getTargetRank() : rank;
        int change = Math.clamp((long) (target - rank) * RANK_CONFIDENCE, -MAX_RANK_PENALTY, MAX_RANK_BONUS);
        List<String> notes = new ArrayList<>();
        notes.add("Lig " + rank + ". sırada bitti (hedef: en kötü " + target + ".)");
        if (rank == 1) {
            change += CHAMPION_BONUS;
            notes.add("Şampiyonluk!");
        }
        if (team.getBudget() != null && team.getBudget() < 0) {
            change -= NEGATIVE_BUDGET_PENALTY;
            notes.add("Bütçe eksiye düştü");
        }
        adjust(manager, change);
        inboxService.post(InboxMessageType.BOARD, "Yönetim kurulu sezonu değerlendirdi",
                String.join(". ", notes) + ". Güven " + (change >= 0 ? "+" : "") + change + " → " + manager.getConfidence()
                        + "/100.", null, team, null);
        if (relegatedIds.contains(team.getId())) {
            sack(manager, "Küme düştü", relegatedIds);
        } else if (manager.getConfidence() <= 0) {
            sack(manager, "Güven kalmadı", relegatedIds);
        }
    }

    // ---- Gelen kutusu işlemleri

    public ActionResponse accept(Long messageId) {
        InboxMessage message = openMessage(messageId);
        ActionResponse response = switch (message.getType()) {
            case TRANSFER_OFFER -> {
                transferService.acceptManagerOffer(message.getPlayer(), message.getTeam(), message.getAmount());
                yield new ActionResponse("DONE", message.getAmount(), message.getPlayer().getName() + " "
                        + MatchCommentary.dative(message.getTeam().getName()) + " satıldı");
            }
            case JOB_OFFER -> acceptJob(message);
            case YOUTH -> promote(message.getPlayer());
            default -> throw new IllegalArgumentException("Bu mesaj kabul edilecek bir teklif değil");
        };
        message.setResolved(true);
        message.setRead(true);
        return response;
    }

    public ActionResponse reject(Long messageId) {
        InboxMessage message = openMessage(messageId);
        if (message.getType() == InboxMessageType.YOUTH) {
            // Bırakılan altyapı genci futbolu bırakır
            message.getPlayer().setActive(false);
            message.getPlayer().setAcademyTeam(null);
        }
        message.setResolved(true);
        message.setRead(true);
        return new ActionResponse("DONE", null, "Reddedildi");
    }

    /** Sözleşme uzatma teklifi (yalnızca yönetilen takımın oyuncusu). */
    public ActionResponse renewContract(Long playerId, ContractOfferRequest request) {
        Team team = requireTeam();
        Player player = playerRepository.findById(playerId)
                .filter(found -> found.isActive() && found.getTeam() != null && found.getTeam().getId().equals(team.getId()))
                .orElseThrow(() -> new PlayerNotFoundException(playerId));
        boolean key = playerRepository.findByTeamIdAndActiveTrueOrderByShirtNumber(team.getId()).stream()
                .sorted(Comparator.comparingInt(Player::getStrength).reversed())
                .limit(TransferService.KEY_PLAYERS)
                .anyMatch(candidate -> candidate.getId().equals(playerId));
        EconomyService.RenewalResult result = economyService.renew(player, request.weeklyWage(), request.seasons(), key);
        return new ActionResponse(result.status(), result.demand(), result.message());
    }

    /** Oyuncuyu satışa çıkar: alıcı takımlar gelen kutusuna teklif gönderir. */
    public ActionResponse listForSale(Long playerId) {
        Team team = requireTeam();
        Player player = playerRepository.findById(playerId)
                .filter(found -> found.isActive() && found.getTeam() != null && found.getTeam().getId().equals(team.getId()))
                .orElseThrow(() -> new PlayerNotFoundException(playerId));
        int offers = transferService.listForSale(player);
        return new ActionResponse("DONE", (long) offers, offers == 0
                ? "Bu bedeli ödeyebilecek bir alıcı çıkmadı"
                : offers + " takım teklif gönderdi, gelen kutuna bak");
    }

    @Transactional(readOnly = true)
    public List<Player> academyProspects() {
        return playerRepository.findByAcademyTeamIdAndActiveTrue(requireTeam().getId());
    }

    // ---- Kariyer özeti

    @Transactional(readOnly = true)
    public CareerResponse career() {
        ManagerProfile profile = managerProfileRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new ManagerModeException("Henüz menajerlik yapmadın"));
        int latest = currentSeasonNumber();
        List<Season> seasons = seasonRepository.findAllByOrderBySeasonNumberDesc();
        List<CareerResponse.Spell> spells = new ArrayList<>();
        for (ManagerSpell spell : managerSpellRepository.findAllByOrderByIdAsc()) {
            int end = spell.getEndSeason() != null ? spell.getEndSeason() : latest;
            int[] record = new int[3];
            int titles = 0;
            int cups = 0;
            for (Season season : seasons) {
                if (season.getSeasonNumber() < spell.getStartSeason() || season.getSeasonNumber() > end) {
                    continue;
                }
                List<Match> matches = new ArrayList<>(matchRepository.findLeagueMatchesBySeason(season.getId()));
                matches.addAll(matchRepository.findCupMatchesBySeason(season.getId()));
                for (Match match : matches) {
                    if (match.isPlayed() && involves(match, spell.getTeam())) {
                        Team winner = match.winner();
                        record[winner == null ? 1 : winner.getId().equals(spell.getTeam().getId()) ? 0 : 2]++;
                    }
                }
                titles += season.getChampion() != null && season.getChampion().getId().equals(spell.getTeam().getId()) ? 1 : 0;
                cups += season.getCupWinner() != null && season.getCupWinner().getId().equals(spell.getTeam().getId()) ? 1 : 0;
            }
            spells.add(new CareerResponse.Spell(spell.getTeam().getId(), spell.getTeam().getName(),
                    spell.getStartSeason(), spell.getEndSeason(), spell.getEndReason(), record[0] + record[1] + record[2],
                    record[0], record[1], record[2], titles, cups));
        }
        return new CareerResponse(profile.getName(), spells,
                spells.stream().mapToInt(CareerResponse.Spell::matches).sum(),
                spells.stream().mapToInt(CareerResponse.Spell::wins).sum(),
                spells.stream().mapToInt(CareerResponse.Spell::draws).sum(),
                spells.stream().mapToInt(CareerResponse.Spell::losses).sum(),
                spells.stream().mapToInt(CareerResponse.Spell::leagueTitles).sum(),
                spells.stream().mapToInt(CareerResponse.Spell::cups).sum());
    }

    // ---- Yardımcılar

    /**
     * Hedefler takımın gücüne göre: güç sırası + 1 (küme düşme hattının üstü); güç sırası ilk 2 ise kupada final,
     * ilk 4 ise yarı final, ilk 8 ise çeyrek final (kupaya katılmak).
     */
    void setObjectives(ManagerProfile manager, Team team, Season season) {
        List<Team> teams = teamRepository.findByActiveTrue().stream()
                .sorted(Comparator.comparingInt(Team::matchStrength).reversed())
                .toList();
        int expected = teams.indexOf(team) + 1;
        int safeRank = Math.max(1, teams.size() - RELEGATION_SPOTS);
        manager.setTargetRank(Math.min(safeRank, expected + 1));
        manager.setCupTarget(expected <= 2 ? CupRound.FINAL : expected <= 4 ? CupRound.SEMI_FINAL
                : expected <= 8 ? CupRound.QUARTER_FINAL : null);
        manager.setObjectiveSeason(season.getSeasonNumber());
        inboxService.post(InboxMessageType.BOARD, "Sezon " + season.getSeasonNumber() + " hedefleri",
                "Ligi en kötü " + manager.getTargetRank() + ". sırada bitir"
                        + (manager.getCupTarget() != null ? "; kupada en az " + roundName(manager.getCupTarget()) + "e ulaş" : "")
                        + "; bütçeyi eksiye düşürme. Güven: " + manager.getConfidence() + "/100.", null, team, null);
    }

    /** Kupa hedefi: takımın oynadığı en ileri tur hedef tura ulaştı mı (kupa oynanmadıysa değerlendirilmez). */
    private void evaluateCup(ManagerProfile manager, Team team, int seasonNumber) {
        Optional<Season> season = seasonRepository.findAllByOrderBySeasonNumberDesc().stream()
                .filter(found -> found.getSeasonNumber() == seasonNumber).findFirst();
        if (season.isEmpty()) {
            return;
        }
        List<Match> cup = matchRepository.findCupMatchesBySeason(season.get().getId());
        if (cup.isEmpty()) {
            return;
        }
        Optional<CupRound> reached = cup.stream().filter(match -> involves(match, team))
                .map(match -> match.getMatchWeek().getCupRound())
                .max(Comparator.naturalOrder());
        boolean met = reached.isPresent() && reached.get().ordinal() >= manager.getCupTarget().ordinal();
        adjust(manager, met ? CUP_TARGET_MET : -CUP_TARGET_MISSED);
        inboxService.post(InboxMessageType.BOARD, met ? "Kupa hedefi tuttu" : "Kupa hedefi tutmadı",
                "Hedef: " + roundName(manager.getCupTarget()) + ". Güven " + (met ? "+" + CUP_TARGET_MET : "-" + CUP_TARGET_MISSED)
                        + " → " + manager.getConfidence() + "/100.", null, team, null);
    }

    /**
     * Görevden alma: takıma yapay zekâ hocası gelir, menajere birkaç (daha küçük) takımdan iş teklifi gelir;
     * excludedTeamIds: o anda küme düşen takımlar (teklif yapamaz).
     */
    void sack(ManagerProfile manager, String reason, List<Long> excludedTeamIds) {
        Team team = manager.getTeam();
        endSpell(reason);
        managerService.hireFor(team);
        manager.setTeam(null);
        manager.setObjectiveSeason(null);
        inboxService.post(InboxMessageType.SACKED, "Görevine son verildi",
                team.getName() + " yönetimi yollarını ayırdı (" + reason + "). Gelen iş tekliflerini değerlendirebilirsin.",
                null, team, null);
        List<Team> eligible = teamRepository.findByActiveTrue().stream()
                .filter(candidate -> !candidate.getId().equals(team.getId()))
                .filter(candidate -> !excludedTeamIds.contains(candidate.getId()))
                .filter(candidate -> !candidate.isBigFour() || team.isBigFour())
                .sorted(Comparator.comparingInt(Team::getStrength))
                .toList();
        List<Team> candidates = new ArrayList<>(eligible.stream()
                .filter(candidate -> candidate.getStrength() <= team.getStrength() + JOB_STRENGTH_MARGIN)
                .toList());
        // Kovulan takımdan zayıf yeterince takım yoksa ligin en zayıf takımları teklif yapar
        if (candidates.size() < JOB_OFFERS) {
            candidates = new ArrayList<>(eligible.subList(0, Math.min(JOB_OFFERS, eligible.size())));
        }
        Collections.shuffle(candidates, ThreadLocalRandom.current());
        for (Team candidate : candidates.subList(0, Math.min(JOB_OFFERS, candidates.size()))) {
            inboxService.post(InboxMessageType.JOB_OFFER, candidate.getName() + " seni teknik direktör olarak istiyor",
                    candidate.getName() + " (güç " + candidate.getStrength() + ") sana görev teklif ediyor.", null,
                    candidate, null);
        }
    }

    private ActionResponse acceptJob(InboxMessage message) {
        ManagerProfile profile = managerProfileRepository.findFirstByOrderByIdAsc().orElseThrow();
        if (profile.getTeam() != null) {
            throw new IllegalArgumentException("Zaten bir takımı yönetiyorsun");
        }
        Team team = message.getTeam();
        if (!team.isActive()) {
            throw new IllegalArgumentException(team.getName() + " artık ligde değil");
        }
        team.setManager(null);
        profile.setTeam(team);
        startSpell(profile, team);
        inboxMessageRepository.findByTypeAndResolvedFalse(InboxMessageType.JOB_OFFER).forEach(other -> other.setResolved(true));
        return new ActionResponse("DONE", null, team.getName() + " teknik direktörü oldun");
    }

    private ActionResponse promote(Player prospect) {
        Team team = prospect.getAcademyTeam();
        List<Player> squad = playerRepository.findByTeamIdAndActiveTrueOrderByShirtNumber(team.getId());
        if (squad.size() >= TransferService.MAX_SQUAD) {
            throw new IllegalArgumentException("Kadro dolu (en fazla " + TransferService.MAX_SQUAD + " oyuncu)");
        }
        prospect.setTeam(team);
        prospect.setAcademyTeam(null);
        prospect.setShirtNumber(SquadGenerator.freeShirtNumber(
                squad.stream().map(Player::getShirtNumber).collect(Collectors.toSet()),
                prospect.getPosition()));
        economyService.signNewPlayers(List.of(prospect));
        return new ActionResponse("DONE", null, prospect.getName() + " A takıma yükseldi");
    }

    private InboxMessage openMessage(Long id) {
        InboxMessage message = inboxMessageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mesaj bulunamadı"));
        if (message.isResolved()) {
            throw new IllegalArgumentException("Bu teklif artık geçerli değil");
        }
        return message;
    }

    private Optional<ManagerProfile> managedProfile() {
        return managerProfileRepository.findFirstByOrderByIdAsc().filter(profile -> profile.getTeam() != null);
    }

    private Team requireTeam() {
        return managedProfile().map(ManagerProfile::getTeam)
                .orElseThrow(() -> new ManagerModeException("\"Takımım\" modu kapalı"));
    }

    private static void adjust(ManagerProfile manager, int delta) {
        manager.setConfidence(Math.clamp((long) manager.getConfidence() + delta, 0, MAX_CONFIDENCE));
    }

    private int currentSeasonNumber() {
        return seasonRepository.findTopByOrderBySeasonNumberDesc().map(Season::getSeasonNumber).orElse(1);
    }

    private static boolean involves(Match match, Team team) {
        return match.getHomeTeam().getId().equals(team.getId()) || match.getAwayTeam().getId().equals(team.getId());
    }

    static String roundName(CupRound round) {
        return switch (round) {
            case QUARTER_FINAL -> "çeyrek final";
            case SEMI_FINAL -> "yarı final";
            case FINAL -> "final";
        };
    }
}
