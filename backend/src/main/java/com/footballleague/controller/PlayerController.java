package com.footballleague.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.footballleague.dto.PlayerProfileResponse;
import com.footballleague.dto.PlayerRequest;
import com.footballleague.dto.PlayerResponse;
import com.footballleague.dto.PlayerStatsResponse;
import com.footballleague.service.PlayerService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
@Tag(name = "Players", description = "Oyuncu düzenleme ve sezon oyuncu istatistikleri")
public class PlayerController {

    private final PlayerService playerService;

    @GetMapping("/stats")
    public List<PlayerStatsResponse> getSeasonStats(@RequestParam(required = false) Long seasonId) {
        return playerService.getSeasonStats(seasonId);
    }

    @GetMapping("/{id}")
    public PlayerProfileResponse getProfile(@PathVariable Long id) {
        return playerService.getProfile(id);
    }

    @PutMapping("/{id}")
    public PlayerResponse updatePlayer(@PathVariable Long id, @Valid @RequestBody PlayerRequest request) {
        return playerService.updatePlayer(id, request);
    }
}
