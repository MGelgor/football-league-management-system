package com.footballleague.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public List<StandingResponse> getStandings() {
        return standingsService.getStandings();
    }
}
