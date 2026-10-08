package com.footballleague.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.footballleague.dto.StandingResponse;
import com.footballleague.service.StandingsService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/standings")
@RequiredArgsConstructor
@Tag(name = "Standings", description = "Puan durumu (Puan > Averaj > Atılan gol)")
public class StandingsController {

    private final StandingsService standingsService;

    @GetMapping
    /** division: 1 (1. Lig, varsayılan) ya da 2 (2. Lig). */
    public List<StandingResponse> getStandings(@RequestParam(required = false) Long seasonId,
            @RequestParam(defaultValue = "1") int division) {
        return standingsService.getStandings(seasonId, StandingsService.checkDivision(division));
    }
}
