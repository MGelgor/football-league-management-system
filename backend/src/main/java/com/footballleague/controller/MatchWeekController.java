package com.footballleague.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.footballleague.dto.MatchWeekResponse;
import com.footballleague.service.MatchSimulationService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/weeks")
@RequiredArgsConstructor
@Tag(name = "Match Weeks", description = "Hafta simülasyonu (\"Haftayı Oynat\")")
public class MatchWeekController {

    private final MatchSimulationService matchSimulationService;

    @PostMapping("/{weekNumber}/play")
    public MatchWeekResponse playWeek(@PathVariable Integer weekNumber) {
        return matchSimulationService.playWeek(weekNumber);
    }
}
