package com.footballleague.config;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.footballleague.entity.Room;
import com.footballleague.exception.ErrorResponse;
import com.footballleague.service.RoomScheduler;
import com.footballleague.service.RoomService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * /api isteklerinin hangi odaya ait olduğunu belirler: X-Room-Code / X-Room-Token başlıkları
 * (EventSource ve indirme linkleri başlık gönderemediği için ?room=&token= de olur). Token odanın bir üyesine
 * aitse RoomContext doldurulur; veri değiştiren istekleri yalnızca oda sahibi yapabilir.
 * Odada ligi zamanlayıcı ilerletir: maç oynatan / sezonu değiştiren uçlar kapalıdır; zamanı gelmiş maç
 * günleri isteğin başında işlenir (sunucu uyuduysa ilk gelen istek takvimi yetiştirir).
 * Oda belirtilmezse varsayılan (tek kişilik) lig kullanılır; app.rooms.required=true ise istek reddedilir.
 * /api/rooms uçları odanın dışında çalışır (kendi token kontrollerini yapar).
 */
@Component
public class RoomFilter extends OncePerRequestFilter {

    public static final String ROOM_HEADER = "X-Room-Code";
    public static final String TOKEN_HEADER = "X-Room-Token";
    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD");
    // Odada yalnızca zamanlayıcının yapabileceği işler ("YÖNTEM /yol" öneki)
    private static final List<String> SCHEDULER_ONLY = List.of("POST /api/fixtures/generate", "DELETE /api/fixtures",
            "POST /api/seasons/play-all", "POST /api/cup/", "POST /api/data/import", "POST /api/my-team/play",
            "POST /api/my-team/live/");
    private static final Pattern PLAY_WEEK = Pattern.compile("POST /api/weeks/\\d+/play");
    // Üyelerin kendi takımı için yapabileceği değişiklikler (servisler takımın üyeye ait olduğunu denetler)
    private static final List<String> MEMBER_PATHS = List.of("/api/my-team", "/api/transfers/offers",
            "/api/transfers/free-agents/", "/api/transfers/bids");
    private static final Pattern TACTICS = Pattern.compile("PUT /api/teams/\\d+/tactics");
    // Maç gününe 5 dakika kala kilitlenen değişiklikler
    private static final Pattern LINEUP_CHANGE = Pattern.compile("PUT /api/my-team/lineup|PUT /api/teams/\\d+/tactics");
    // Maç günü canlı yayınlanırken sonuç göstermeyen, okunabilir uçlar (canlı yayın ayrıca açık)
    private static final Set<String> OPEN_DURING_LIVE = Set.of("/api/teams", "/api/my-team/lineup",
            "/api/transfers/window", "/api/referees");

    private final RoomService roomService;
    private final RoomScheduler roomScheduler;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final boolean required;

    public RoomFilter(RoomService roomService, RoomScheduler roomScheduler, ObjectMapper objectMapper, Clock clock,
            @Value("${app.rooms.required}") boolean required) {
        this.roomService = roomService;
        this.roomScheduler = roomScheduler;
        this.clock = clock;
        this.objectMapper = objectMapper;
        this.required = required;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/") || path.startsWith("/api/rooms") || "OPTIONS".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String code = firstNonBlank(request.getHeader(ROOM_HEADER), request.getParameter("room"));
        if (code == null) {
            if (required) {
                reject(response, HttpStatus.UNAUTHORIZED, "Önce bir odaya katılın");
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        String token = firstNonBlank(request.getHeader(TOKEN_HEADER), request.getParameter("token"));
        Optional<RoomContext.Current> room = roomService.authenticate(code, token);
        if (room.isEmpty()) {
            reject(response, HttpStatus.FORBIDDEN, "Bu odaya erişiminiz yok. Davet koduyla yeniden katılın.");
            return;
        }
        String action = request.getMethod() + " " + request.getRequestURI();
        if (SCHEDULER_ONLY.stream().anyMatch(action::startsWith) || PLAY_WEEK.matcher(action).matches()) {
            reject(response, HttpStatus.FORBIDDEN, "Odada maçları zamanlayıcı oynatır (Odalar sayfasındaki takvim)");
            return;
        }
        boolean memberAction = MEMBER_PATHS.stream().anyMatch(request.getRequestURI()::startsWith)
                || TACTICS.matcher(action).matches();
        if (!READ_METHODS.contains(request.getMethod()) && !room.get().owner() && !memberAction) {
            reject(response, HttpStatus.FORBIDDEN, "Bu işlemi yalnızca oda sahibi yapabilir");
            return;
        }

        if (roomScheduler.tick(room.get().roomId()) > 0) {
            // Maç günü oynandı: kilit zamanı sıradaki güne geçti
            room = roomService.authenticate(code, token);
            if (room.isEmpty()) {
                reject(response, HttpStatus.FORBIDDEN, "Bu odaya erişiminiz yok. Davet koduyla yeniden katılın.");
                return;
            }
        }
        Instant lockAt = room.map(RoomContext.Current::lineupLockAt).orElse(null);
        if (LINEUP_CHANGE.matcher(action).matches() && lockAt != null && !clock.instant().isBefore(lockAt)) {
            reject(response, HttpStatus.CONFLICT,
                    "Kadro ve taktik maça 5 dakika kala kilitlenir; maç oynandıktan sonra sıradaki gün için değiştirebilirsin");
            return;
        }

        RoomContext.Current current = room.get();
        if (READ_METHODS.contains(request.getMethod()) && revealsLiveResults(current, request.getRequestURI())) {
            long minutes = Math.max(1, Duration.between(clock.instant(),
                    current.liveStartedAt().plus(Room.LIVE_DURATION)).toMinutes() + 1);
            reject(response, HttpStatus.CONFLICT, "Maç günü canlı oynanıyor; sonuçlar yayın bitince (yaklaşık "
                    + minutes + " dk sonra) açılır. Canlı sayfasından izleyebilirsin.");
            return;
        }

        RoomContext.set(current);
        try {
            chain.doFilter(request, response);
        } finally {
            RoomContext.clear();
        }
    }

    /** Canlı yayın sürerken skor / puan / olay gösterebilecek okuma isteği mi (yayının kendisi hariç). */
    private boolean revealsLiveResults(RoomContext.Current room, String path) {
        if (room.liveWeek() == null || room.liveStartedAt() == null
                || !clock.instant().isBefore(room.liveStartedAt().plus(Room.LIVE_DURATION))) {
            return false;
        }
        return !OPEN_DURING_LIVE.contains(path) && !path.equals("/api/weeks/" + room.liveWeek() + "/live");
    }

    private void reject(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(message));
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second == null || second.isBlank() ? null : second;
    }
}
