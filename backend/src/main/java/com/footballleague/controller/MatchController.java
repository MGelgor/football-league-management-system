package com.footballleague.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.footballleague.dto.MatchDetailResponse;
import com.footballleague.service.MatchDetailService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
@Tag(name = "Matches", description = "Maç detayı: olaylar (gol, asist, kart) ve takım istatistikleri")
public class MatchController {

    private final MatchDetailService matchDetailService;

    @GetMapping("/{id}")
    public MatchDetailResponse getMatch(@PathVariable Long id) {
        return matchDetailService.getMatchDetail(id);
    }
}
