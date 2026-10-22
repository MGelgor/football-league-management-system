package com.footballleague.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.footballleague.service.RoomScheduler;

import lombok.RequiredArgsConstructor;

/** Sunucu uyanıkken her dakika zamanı gelen oda olaylarını işler (testlerde kapalı). */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.rooms.scheduler-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class RoomTickJob {

    private final RoomScheduler roomScheduler;

    @Scheduled(fixedDelayString = "${app.rooms.tick-interval-ms:60000}", initialDelay = 30_000)
    public void tick() {
        roomScheduler.tickAll();
    }
}
