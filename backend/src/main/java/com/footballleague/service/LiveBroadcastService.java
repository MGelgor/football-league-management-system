package com.footballleague.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.footballleague.dto.LiveTimeline;
import com.footballleague.entity.Match;
import com.footballleague.entity.MatchAppearance;
import com.footballleague.entity.MatchEvent;
import com.footballleague.entity.MatchEventType;
import com.footballleague.entity.MatchWeek;
import com.footballleague.entity.Season;
import com.footballleague.exception.FixtureNotGeneratedException;
import com.footballleague.exception.MatchWeekNotFoundException;
import com.footballleague.exception.WeekNotPlayedException;
import com.footballleague.repository.MatchAppearanceRepository;
import com.footballleague.repository.MatchEventRepository;
import com.footballleague.repository.MatchRepository;
import com.footballleague.repository.MatchWeekRepository;
import com.footballleague.repository.SeasonRepository;

import lombok.RequiredArgsConstructor;

/**
 * Oynanmış bir haftayı Server-Sent Events ile "canlı" yayınlar: maçlar backend'de bir anda simüle edilir,
 * burada olaylar dakika dakika, seçilen hızda istemciye itilir. Her "minute" olayının id'si dakikadır;
 * bağlantı koparsa tarayıcı Last-Event-ID ile yeniden bağlanır ve yayın kaldığı dakikadan sürer.
 */
@Service
@RequiredArgsConstructor
public class LiveBroadcastService {

    static final int MATCH_MINUTES = 90;
    // 1x hızda bir maç 30 saniye sürer
    private static final long MATCH_DURATION_MS = 30_000;
    private static final long EMITTER_TIMEOUT_MS = 5 * 60_000;
    // 0 = beklemeden hepsini gönder ("Atla")
    public static final Set<Integer> SPEEDS = Set.of(0, 1, 3, 10);

    private final SeasonRepository seasonRepository;
    private final MatchWeekRepository matchWeekRepository;
    private final MatchRepository matchRepository;
    private final MatchEventRepository matchEventRepository;
    private final MatchAppearanceRepository matchAppearanceRepository;

    /** Güncel sezonun haftası (kupa turları 101+); haftadaki maçların hepsi oynanmış olmalı. */
    @Transactional(readOnly = true)
    public LiveTimeline timeline(int weekNumber) {
        Season season = seasonRepository.findTopByOrderBySeasonNumberDesc().orElseThrow(FixtureNotGeneratedException::new);
        MatchWeek week = matchWeekRepository.findBySeasonIdAndWeekNumber(season.getId(), weekNumber)
                .orElseThrow(() -> new MatchWeekNotFoundException(weekNumber));
        List<Match> matches = matchRepository.findByMatchWeekIdOrderById(week.getId());
        if (matches.isEmpty() || !matches.stream().allMatch(Match::isPlayed)) {
            throw new WeekNotPlayedException(weekNumber);
        }

        List<LiveTimeline.LiveMatch> liveMatches = new ArrayList<>();
        List<LiveTimeline.LiveItem> items = new ArrayList<>();
        for (Match match : matches) {
            liveMatches.add(new LiveTimeline.LiveMatch(match.getId(), match.getHomeTeam().getId(),
                    match.getHomeTeam().getName(), match.getAwayTeam().getId(), match.getAwayTeam().getName(),
                    match.getHomeScore(), match.getAwayScore(), match.getHomePenalties(), match.getAwayPenalties()));
            items.addAll(matchItems(match));
        }
        // Aynı dakikada önce olaylar, sonra değişiklikler (sakatlanan oyuncunun yerine giren sonra görünsün)
        items.sort(Comparator.comparingInt(LiveTimeline.LiveItem::minute)
                .thenComparing(item -> item.type().equals("SUBSTITUTION")));
        return new LiveTimeline(weekNumber, liveMatches, items);
    }

    private List<LiveTimeline.LiveItem> matchItems(Match match) {
        return buildItems(match.getId(), match.getHomeTeam().getId(), match.getHomeTeam().getName(),
                match.getAwayTeam().getName(), matchEventRepository.findByMatchIdWithPlayers(match.getId()),
                matchAppearanceRepository.findByMatchIdWithPlayers(match.getId()));
    }

    /**
     * Olaylar ve oyuna girenlerden yorumlu yayın satırları (veritabanından ya da kullanıcının canlı maçının
     * bellekteki kayıtlarından). Takım adları parametreyle gelir; takım nesnesinin yalnızca id'si okunur.
     */
    static List<LiveTimeline.LiveItem> buildItems(Long matchId, Long homeTeamId, String homeName, String awayName,
            List<MatchEvent> events, List<MatchAppearance> appearances) {
        List<LiveTimeline.LiveItem> items = new ArrayList<>();
        for (MatchEvent event : events) {
            boolean home = event.getTeam().getId().equals(homeTeamId);
            String assist = event.getAssistPlayer() != null ? event.getAssistPlayer().getName() : null;
            String team = home ? homeName : awayName;
            items.add(new LiveTimeline.LiveItem(matchId, event.getMinute(), event.getType().name(), home,
                    event.getPlayer().getId(), event.getPlayer().getName(), assist,
                    MatchCommentary.describe(event.getType(), event.getMinute(), event.getPlayer().getName(), assist,
                            team, event.isPenalty()), event.isPenalty()));
        }
        for (MatchAppearance appearance : appearances) {
            if (appearance.isStarter() || appearance.getReplacedPlayer() == null) {
                continue;
            }
            boolean home = appearance.getTeam().getId().equals(homeTeamId);
            String team = home ? homeName : awayName;
            items.add(new LiveTimeline.LiveItem(matchId, appearance.getMinuteOn(), "SUBSTITUTION", home,
                    appearance.getPlayer().getId(), appearance.getPlayer().getName(),
                    appearance.getReplacedPlayer().getName(),
                    MatchCommentary.substitution(appearance.getPlayer().getName(),
                            appearance.getReplacedPlayer().getName(), team), false));
        }
        items.sort(Comparator.comparingInt(LiveTimeline.LiveItem::minute)
                .thenComparing(item -> item.type().equals("SUBSTITUTION")));
        return items;
    }

    /**
     * fromMinute'ten 90'a kadar her dakika için bir "minute" olayı gönderir: önce "start" (maçlar), en sonda
     * "end" (penaltılar dahil sonuçlar). speed: 1 / 3 / 10 kat hız ya da 0 (beklemeden).
     */
    public SseEmitter stream(LiveTimeline timeline, int speed, int fromMinute) {
        if (!SPEEDS.contains(speed)) {
            throw new IllegalArgumentException("Hız 0, 1, 3 ya da 10 olmalı");
        }
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        long delayMs = speed == 0 ? 0 : MATCH_DURATION_MS / MATCH_MINUTES / speed;
        Thread.ofVirtual().start(() -> {
            try {
                emitter.send(SseEmitter.event().name("start").data(timeline.matches(), MediaType.APPLICATION_JSON));
                for (LiveTimeline.LiveMinute minute : minutes(timeline, Math.max(0, fromMinute))) {
                    if (delayMs > 0) {
                        Thread.sleep(delayMs);
                    }
                    emitter.send(SseEmitter.event().name("minute").id(String.valueOf(minute.minute()))
                            .data(minute, MediaType.APPLICATION_JSON));
                }
                emitter.send(SseEmitter.event().name("end").data(timeline.matches(), MediaType.APPLICATION_JSON));
                emitter.complete();
            } catch (IOException | IllegalStateException e) {
                // İstemci bağlantıyı kapattı
                emitter.completeWithError(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                emitter.complete();
            }
        });
        return emitter;
    }

    /** fromMinute..90 arası dakikalar; skorlar 0. dakikadan itibaren biriktirilir (yeniden bağlanınca da doğru). */
    static List<LiveTimeline.LiveMinute> minutes(LiveTimeline timeline, int fromMinute) {
        Map<Long, int[]> scores = new HashMap<>();
        timeline.matches().forEach(match -> scores.put(match.matchId(), new int[2]));
        Map<Integer, List<LiveTimeline.LiveItem>> byMinute = new HashMap<>();
        timeline.items().forEach(item -> byMinute.computeIfAbsent(item.minute(), m -> new ArrayList<>()).add(item));

        List<LiveTimeline.LiveMinute> minutes = new ArrayList<>();
        for (int minute = 0; minute <= MATCH_MINUTES; minute++) {
            List<LiveTimeline.LiveItem> items = byMinute.getOrDefault(minute, List.of());
            for (LiveTimeline.LiveItem item : items) {
                // Kendi kalesine gol rakibin hanesine yazılır
                if (item.type().equals(MatchEventType.GOAL.name())) {
                    scores.get(item.matchId())[item.home() ? 0 : 1]++;
                } else if (item.type().equals(MatchEventType.OWN_GOAL.name())) {
                    scores.get(item.matchId())[item.home() ? 1 : 0]++;
                }
            }
            if (minute >= fromMinute) {
                minutes.add(new LiveTimeline.LiveMinute(minute, phase(minute), items,
                        timeline.matches().stream()
                                .map(match -> new LiveTimeline.LiveScore(match.matchId(),
                                        scores.get(match.matchId())[0], scores.get(match.matchId())[1]))
                                .toList()));
            }
        }
        return minutes;
    }

    private static String phase(int minute) {
        return switch (minute) {
            case 0 -> "KICK_OFF";
            case 45 -> "HALF_TIME";
            case MATCH_MINUTES -> "FULL_TIME";
            default -> null;
        };
    }
}
