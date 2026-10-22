package com.footballleague.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.footballleague.service.RoomScheduler;

import lombok.RequiredArgsConstructor;

/**
 * Harici cron (GitHub Actions) bunu birkaç dakikada bir çağırır: uyuyan sunucuyu uyandırır ve zamanı gelen
 * maç günlerini oynatır. Gizli anahtar gerekmez: yalnızca zamanı gelmiş olayları işler, sık çağrılırsa atlar.
 */
@RestController
@RequiredArgsConstructor
public class InternalController {

    private final RoomScheduler roomScheduler;

    @GetMapping("/internal/tick")
    public Map<String, Object> tick() {
        Integer events = roomScheduler.tickAllThrottled();
        return events == null ? Map.of("skipped", true) : Map.of("events", events);
    }
}
