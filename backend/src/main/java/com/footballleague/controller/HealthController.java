package com.footballleague.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Barındırma servisinin "uygulama ayakta mı?" kontrolü (RoomFilter dışında, oda gerekmez). */
@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
