package com.footballleague.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.footballleague.dto.MatchWeekResponse;
import com.footballleague.service.LiveBroadcastService;
import com.footballleague.service.MatchSimulationService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/weeks")
@RequiredArgsConstructor
@Tag(name = "Match Weeks", description = "Hafta simülasyonu (\"Haftayı Oynat\") ve oynanmış haftanın canlı yayını")
public class MatchWeekController {

    private final MatchSimulationService matchSimulationService;
    private final LiveBroadcastService liveBroadcastService;

    /** auto=true: yönetilen takımın maçı için kadro seçilmediyse yapay zekâ seçer. */
    @PostMapping("/{weekNumber}/play")
    public MatchWeekResponse playWeek(@PathVariable Integer weekNumber,
            @RequestParam(defaultValue = "false") boolean auto) {
        return matchSimulationService.playWeek(weekNumber, auto);
    }

    /**
     * Oynanmış haftanın (kupa turları 101+) Server-Sent Events yayını. Yeniden bağlanan tarayıcı Last-Event-ID
     * başlığını gönderir; yayın o dakikadan sonra devam eder. from ile de başlangıç dakikası seçilebilir.
     */
    @GetMapping("/{weekNumber}/live")
    public SseEmitter live(@PathVariable Integer weekNumber, @RequestParam(defaultValue = "1") int speed,
            @RequestParam(required = false) Integer from,
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {
        int fromMinute = from != null ? from : 0;
        if (lastEventId != null && lastEventId.matches("\\d+")) {
            fromMinute = Integer.parseInt(lastEventId) + 1;
        }
        return liveBroadcastService.stream(liveBroadcastService.timeline(weekNumber), speed, fromMinute);
    }
}
