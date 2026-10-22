package com.footballleague.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.footballleague.config.RoomContext;
import com.footballleague.dto.LiveSessionResponse;
import com.footballleague.dto.LiveTimeline;
import com.footballleague.dto.MatchResponse;
import com.footballleague.dto.SecondHalfRequest;
import com.footballleague.entity.Competition;
import com.footballleague.entity.Match;
import com.footballleague.entity.MatchAppearance;
import com.footballleague.entity.MatchLineup;
import com.footballleague.entity.PlayStyle;
import com.footballleague.entity.Player;
import com.footballleague.entity.Team;
import com.footballleague.exception.ManagerModeException;
import com.footballleague.repository.MatchRepository;
import com.footballleague.repository.PlayerRepository;

import lombok.RequiredArgsConstructor;

/**
 * Kullanıcının maçını iki devre hâlinde oynatır. İlk yarı oynanır ve oturum bellekte tutulur (henüz hiçbir şey
 * kaydedilmez); devre arasında kullanıcı değişiklik, stil ve soyunma odası konuşmasıyla ikinci yarıyı etkiler.
 * İkinci yarı bitince maç kaydedilir, haftanın / kupa turunun kalan maçları oynanır.
 * Her yarının golleri o yarının gol beklentisinin yarısıyla Poisson dağılımından çekilir. İkinci yarıda iki takımın
 * sahadaki oyuncularının ortalama maç içi yorgunluğu gücünden düşülür (dinç yedekler bu kaybı azaltır).
 * Oturum devre arasında bellekte durur; sayfadan ayrılıp dönen kullanıcı {@link #current()} ile kaldığı yere döner.
 */
@Service
@RequiredArgsConstructor
public class InteractiveMatchService {

    static final int SUBSTITUTION_MINUTE = 46;
    // Soyunma odası konuşmasının ikinci yarı gol beklentisine etkisi
    static final double MOTIVATE_BOOST = 1.10;
    static final double CALM_OPPONENT_FACTOR = 0.90;
    static final double CRITICIZE_BOOST = 1.15;
    static final double CRITICIZE_BACKFIRE = 0.92;
    private static final int CRITICIZE_MORALE_COST = 5;
    // İkinci yarıda ortalama maç içi yorgunluğun her 1 puanı takım gücünden bu kadar düşer
    static final double FATIGUE_STRENGTH_WEIGHT = 2;

    // Anahtar: oda şeması + maç id (her odanın maç id'leri 1'den başlar)
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    private final MyTeamService myTeamService;
    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;
    private final TacticsService tacticsService;
    private final ScoreSimulator scoreSimulator;
    private final MatchDetailGenerator matchDetailGenerator;
    private final MatchSimulationService matchSimulationService;
    private final CupService cupService;
    private final MatchMapper matchMapper;

    private record Session(Long matchId, Long teamId, boolean userHome, MatchDetailGenerator.LiveMatch live,
            ScoreSimulator.TeamSetup homeSetup, ScoreSimulator.TeamSetup awaySetup,
            ScoreSimulator.ExpectedGoals expected, ScoreSimulator.Probabilities probabilities,
            LiveTimeline.LiveMatch info, List<Player> available, Map<Long, Double> startStrength) {
    }

    /** İlk yarıyı oynatır ve devre arası durumunu döner; aynı maç için oturum varsa onu döner. */
    @Transactional
    public LiveSessionResponse start() {
        Team team = myTeamService.activeProfile().getTeam();
        Match match = myTeamService.requireNextMatch(team);
        Session existing = sessions.get(key(match));
        if (existing != null) {
            return response(existing, null, true, null, null, null);
        }
        MatchLineup lineup = myTeamService.lineupForMatch(match, team);
        Team home = match.getHomeTeam();
        Team away = match.getAwayTeam();
        boolean userHome = home.getId().equals(team.getId());
        Map<Long, List<Player>> squads = squads(home, away);
        List<Player> homeAvailable = squads.getOrDefault(home.getId(), List.of());
        List<Player> awayAvailable = squads.getOrDefault(away.getId(), List.of());

        ScoreSimulator.TeamSetup homeSetup = tacticsService.setup(home, away, true, userHome ? lineup : null);
        ScoreSimulator.TeamSetup awaySetup = tacticsService.setup(away, home, false, userHome ? null : lineup);
        MatchDetailGenerator.SideSetup homeSide = MatchSimulationService.sideSetup(homeAvailable, homeSetup,
                userHome ? lineup : null, !userHome);
        MatchDetailGenerator.SideSetup awaySide = MatchSimulationService.sideSetup(awayAvailable, awaySetup,
                userHome ? null : lineup, userHome);
        ScoreSimulator.ExpectedGoals expected = MatchSimulationService.withLineupQuality(
                scoreSimulator.expectedGoals(homeSetup, awaySetup), homeSide, awaySide);

        MatchDetailGenerator.LiveMatch live = matchDetailGenerator.start(match, homeSide, awaySide);
        live.play(1, MatchDetailGenerator.HALF_TIME, ScoreSimulator.poissonRandom(expected.home() / 2),
                ScoreSimulator.poissonRandom(expected.away() / 2), expected);

        LiveTimeline.LiveMatch info = new LiveTimeline.LiveMatch(match.getId(), home.getId(), home.getName(),
                away.getId(), away.getName(), 0, 0, null, null);
        // Maç bitince form / üst üste maç güncellenir; ekranda maç başındaki efektif güç gösterilsin
        Map<Long, Double> startStrength = new HashMap<>();
        squads.values().forEach(players -> players.forEach(
                player -> startStrength.put(player.getId(), player.effectiveStrength())));
        Session session = new Session(match.getId(), team.getId(), userHome, live, homeSetup, awaySetup, expected,
                scoreSimulator.probabilities(expected), info, userHome ? homeAvailable : awayAvailable, startStrength);
        sessions.put(key(match), session);
        return response(session, null, false, null, null, null);
    }

    /** Devre arasında bekleyen oturum (sıradaki maç için); yoksa boş. */
    @Transactional(readOnly = true)
    public Optional<LiveSessionResponse> current() {
        try {
            Team team = myTeamService.activeProfile().getTeam();
            return myTeamService.nextMatch(team).map(match -> sessions.get(key(match)))
                    .map(session -> response(session, null, true, null, null, null));
        } catch (ManagerModeException e) {
            return Optional.empty();
        }
    }

    /**
     * Devre arası kararlarını uygular ve ikinci yarıyı oynatır: değişiklikler 46. dakikada, yeni stil gol
     * beklentisini değiştirir, kırmızı kartla eksik kalan takımın beklentisi oyuncu sayısıyla orantılı düşer,
     * konuşma skora göre etkiler. Sonra maç kaydedilir ve haftanın / turun kalanı oynanır.
     */
    @Transactional
    public LiveSessionResponse secondHalf(SecondHalfRequest request) {
        Team team = myTeamService.activeProfile().getTeam();
        Match match = myTeamService.requireNextMatch(team);
        Session session = sessions.get(key(match));
        if (session == null) {
            throw new ManagerModeException("Canlı maç başlatılmadı");
        }
        MatchDetailGenerator.LiveMatch live = session.live();
        boolean userHome = session.userHome();
        for (SecondHalfRequest.Substitution substitution : request.substitutions() == null
                ? List.<SecondHalfRequest.Substitution>of() : request.substitutions()) {
            MatchDetailGenerator.SideView side = live.side(userHome);
            live.substitute(userHome, find(side.onPitch(), substitution.outId()), find(side.bench(), substitution.inId()),
                    SUBSTITUTION_MINUTE);
        }

        ScoreSimulator.TeamSetup userSetup = withStyle(userHome ? session.homeSetup() : session.awaySetup(),
                request.playStyle());
        ScoreSimulator.TeamSetup homeSetup = userHome ? userSetup : session.homeSetup();
        ScoreSimulator.TeamSetup awaySetup = userHome ? session.awaySetup() : userSetup;
        ScoreSimulator.ExpectedGoals base = scoreSimulator.expectedGoals(
                tired(homeSetup, secondHalfFatigue(live, true)), tired(awaySetup, secondHalfFatigue(live, false)));
        double homeLambda = base.home() * pitchFactor(live, true, session);
        double awayLambda = base.away() * pitchFactor(live, false, session);

        int userGoals = userHome ? live.homeGoals() : live.awayGoals();
        int opponentGoals = userHome ? live.awayGoals() : live.homeGoals();
        int moraleBefore = team.getMorale();
        Talk talk = talk(request.talk(), userGoals, opponentGoals);
        if (talk.moraleCost() > 0) {
            team.setMorale(Math.max(0, team.getMorale() - talk.moraleCost()));
        }
        double userLambda = (userHome ? homeLambda : awayLambda) * talk.own();
        double opponentLambda = (userHome ? awayLambda : homeLambda) * talk.opponent();
        homeLambda = userHome ? userLambda : opponentLambda;
        awayLambda = userHome ? opponentLambda : userLambda;

        live.play(SUBSTITUTION_MINUTE, MatchDetailGenerator.MATCH_MINUTES, ScoreSimulator.poissonRandom(homeLambda / 2),
                ScoreSimulator.poissonRandom(awayLambda / 2), new ScoreSimulator.ExpectedGoals(homeLambda, awayLambda));
        MatchDetailGenerator.GeneratedDetail detail = live.finish(session.expected());
        sessions.remove(key(match));

        matchSimulationService.recordManagedResult(match.getId(), detail, session.probabilities(), homeSetup, awaySetup);
        if (match.getMatchWeek().getCompetition() == Competition.LEAGUE) {
            matchSimulationService.playRestOfLeagueWeek(match);
        } else {
            cupService.playRestOfRound(match);
        }
        List<Match> round = matchRepository.findByMatchWeekIdOrderById(match.getMatchWeek().getId());
        return response(session, talk.message(), false, team.getMorale() - moraleBefore, team.getMorale(), round.stream()
                .filter(other -> !other.getId().equals(match.getId()))
                .map(matchMapper::toMatchResponse)
                .toList());
    }

    private record Talk(double own, double opponent, int moraleCost, String message) {
    }

    /** Konuşmanın etkisi skora bağlı: geride / berabereyken motivasyon, öndeyken sakinlik işe yarar. */
    static Talk talk(SecondHalfRequest.TeamTalk talk, int userGoals, int opponentGoals) {
        boolean ahead = userGoals > opponentGoals;
        boolean behind = userGoals < opponentGoals;
        return switch (talk) {
            case NONE -> new Talk(1, 1, 0, null);
            case MOTIVATE -> ahead
                    ? new Talk(1, 1, 0, "Takım zaten öndeydi; konuşma pek bir şey değiştirmedi.")
                    : new Talk(MOTIVATE_BOOST, 1, 0, "Oyuncular ikinci yarıya hırslı çıktı.");
            case CALM -> ahead
                    ? new Talk(1, CALM_OPPONENT_FACTOR, 0, "Takım sakin ve kontrollü; rakibe az pozisyon verecek.")
                    : new Talk(1, 1, 0, "Sakin kalmak skoru değiştirmeye yetmeyebilir.");
            case CRITICIZE -> behind
                    ? new Talk(CRITICIZE_BOOST, 1, CRITICIZE_MORALE_COST, "Sert sözler işe yaradı, oyuncular ateşlendi (moral −5).")
                    : new Talk(CRITICIZE_BACKFIRE, 1, CRITICIZE_MORALE_COST, "Eleştiri ters tepti, oyuncular bozuldu (moral −5).");
        };
    }

    /** Sahadaki oyuncu sayısı (kırmızı kart) ve kullanıcının sahadaki 11'inin kalitesi. */
    private static double pitchFactor(MatchDetailGenerator.LiveMatch live, boolean home, Session session) {
        MatchDetailGenerator.SideView side = live.side(home);
        double manDown = side.onPitch().size() / (double) MatchDetailGenerator.STARTING_PLAYERS;
        if (home != session.userHome() || side.onPitch().isEmpty()) {
            return manDown;
        }
        MatchDetailGenerator.SideSetup current = new MatchDetailGenerator.SideSetup(session.available(),
                (home ? session.homeSetup() : session.awaySetup()).formation(), side.onPitch(), null, false);
        return manDown * TacticsService.lineupQuality(current);
    }

    /**
     * İkinci yarıda sahadaki oyuncuların ortalama maç içi yorgunluğu: devre arasındaki ve maç sonundaki yorgunluğun
     * ortalaması (46'da giren yedek 0'dan başlar).
     */
    static double secondHalfFatigue(MatchDetailGenerator.LiveMatch live, boolean home) {
        return live.side(home).onPitch().stream()
                .mapToDouble(player -> (MatchDetailGenerator.matchFatigue(player,
                        live.minutesPlayed(player, MatchDetailGenerator.HALF_TIME))
                        + MatchDetailGenerator.matchFatigue(player, live.minutesPlayed(player, MatchDetailGenerator.HALF_TIME)
                                + MatchDetailGenerator.MATCH_MINUTES - MatchDetailGenerator.HALF_TIME)) / 2)
                .average().orElse(0);
    }

    private static ScoreSimulator.TeamSetup tired(ScoreSimulator.TeamSetup setup, double fatigue) {
        return new ScoreSimulator.TeamSetup(setup.strength(), setup.morale(), setup.formation(), setup.style(),
                setup.managerBonus() - fatigue * FATIGUE_STRENGTH_WEIGHT);
    }

    private static ScoreSimulator.TeamSetup withStyle(ScoreSimulator.TeamSetup setup, PlayStyle style) {
        return new ScoreSimulator.TeamSetup(setup.strength(), setup.morale(), setup.formation(), style,
                setup.managerBonus());
    }

    private static Player find(List<Player> players, Long id) {
        return players.stream().filter(player -> player.getId().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Değişiklik geçersiz: oyuncu bulunamadı (" + id + ")"));
    }

    private Map<Long, List<Player>> squads(Team home, Team away) {
        return playerRepository.findByTeamIdInAndActiveTrue(List.of(home.getId(), away.getId())).stream()
                .filter(Player::isAvailable)
                .collect(Collectors.groupingBy(player -> player.getTeam().getId()));
    }

    /** moraleChange null değilse maç bitti. */
    private static LiveSessionResponse response(Session session, String talkResult, boolean resumed,
            Integer moraleChange, Integer morale, List<MatchResponse> otherResults) {
        boolean finished = moraleChange != null;
        int minute = finished ? MatchDetailGenerator.MATCH_MINUTES : MatchDetailGenerator.HALF_TIME;
        MatchDetailGenerator.LiveMatch live = session.live();
        LiveTimeline.LiveMatch info = session.info();
        MatchDetailGenerator.SideView side = live.side(session.userHome());
        Map<Player, Double> ratings = new IdentityHashMap<>();
        if (finished) {
            live.appearances().forEach(appearance -> ratings.put(appearance.getPlayer(), appearance.getRating()));
        }
        List<MatchAppearance> appearances = live.appearances();
        ScoreSimulator.TeamSetup userSetup = session.userHome() ? session.homeSetup() : session.awaySetup();
        ScoreSimulator.TeamSetup opponentSetup = session.userHome() ? session.awaySetup() : session.homeSetup();
        Set<Player> stillOn = Collections.newSetFromMap(new IdentityHashMap<>());
        stillOn.addAll(live.side(true).onPitch());
        stillOn.addAll(live.side(false).onPitch());
        Long homeTeamId = info.homeTeamId();
        List<LiveSessionResponse.PitchPlayer> pitch = appearances.stream()
                .map(appearance -> {
                    Player player = appearance.getPlayer();
                    return new LiveSessionResponse.PitchPlayer(player.getId(), player.getName(), player.getPosition(),
                            player.getShirtNumber(), appearance.getTeam().getId().equals(homeTeamId),
                            round1(session.startStrength().get(player.getId())),
                            round1(MatchDetailGenerator.fatigueRate(player)),
                            appearance.getMinuteOn(), stillOn.contains(player) ? null : appearance.getMinuteOff(),
                            appearance.getReplacedPlayer() != null ? appearance.getReplacedPlayer().getId() : null);
                })
                .toList();
        return new LiveSessionResponse(session.matchId(), minute, finished, resumed, info, session.userHome(),
                live.homeGoals(), live.awayGoals(),
                LiveBroadcastService.buildItems(info.matchId(), info.homeTeamId(), info.homeTeamName(),
                        info.awayTeamName(), live.events(), appearances),
                (finished ? onPitchAtEnd(appearances, session) : side.onPitch()).stream()
                        .map(player -> sessionPlayer(player, session, minute, ratings.get(player))).toList(),
                side.bench().stream().map(player -> sessionPlayer(player, session, minute, null)).toList(),
                pitch, side.substitutionsLeft(), userSetup.formation(), opponentSetup.formation(), userSetup.style(),
                talkResult, moraleChange, morale,
                otherResults == null ? List.of() : otherResults);
    }

    /** Maç sonunda kullanıcının takımında oynayan herkes (reytingleriyle). */
    private static List<Player> onPitchAtEnd(List<MatchAppearance> appearances, Session session) {
        return appearances.stream()
                .filter(appearance -> appearance.getTeam().getId().equals(session.teamId()))
                .map(MatchAppearance::getPlayer)
                .toList();
    }

    private static LiveSessionResponse.SessionPlayer sessionPlayer(Player player, Session session, int minute,
            Double rating) {
        double fatigue = MatchDetailGenerator.matchFatigue(player, session.live().minutesPlayed(player, minute));
        return new LiveSessionResponse.SessionPlayer(player.getId(), player.getName(), player.getPosition(),
                player.getShirtNumber(), player.getStrength(), Math.round(player.form() * 100) / 100.0,
                round1(Math.max(1, session.startStrength().get(player.getId()) - fatigue)), round1(fatigue), rating);
    }

    private static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    private static String key(Match match) {
        return RoomContext.schema() + ":" + match.getId();
    }
}
