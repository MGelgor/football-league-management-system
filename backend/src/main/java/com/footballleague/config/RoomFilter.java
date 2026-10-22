package com.footballleague.config;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

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

    private final RoomService roomService;
    private final RoomScheduler roomScheduler;
    private final ObjectMapper objectMapper;
    private final boolean required;

    public RoomFilter(RoomService roomService, RoomScheduler roomScheduler, ObjectMapper objectMapper,
            @Value("${app.rooms.required}") boolean required) {
        this.roomService = roomService;
        this.roomScheduler = roomScheduler;
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
        if (!READ_METHODS.contains(request.getMethod()) && !room.get().owner()) {
            reject(response, HttpStatus.FORBIDDEN, "Bu işlemi yalnızca oda sahibi yapabilir");
            return;
        }

        roomScheduler.tick(room.get().roomId());

        RoomContext.set(room.get());
        try {
            chain.doFilter(request, response);
        } finally {
            RoomContext.clear();
        }
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
