package com.footballleague.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.footballleague.dto.CupResponse;
import com.footballleague.service.CupService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/cup")
@RequiredArgsConstructor
@Tag(name = "Cup", description = "Lig sonrası ilk 8 takımla eleme kupası")
public class CupController {

    private final CupService cupService;

    @GetMapping
    public CupResponse getCup(@RequestParam(required = false) Long seasonId) {
        return cupService.getCup(seasonId);
    }

    @PostMapping("/start")
    public CupResponse startCup() {
        return cupService.startCup();
    }

    @PostMapping("/play-round")
    public CupResponse playRound() {
        return cupService.playRound();
    }

    @PostMapping("/play-all")
    public CupResponse playAll() {
        return cupService.playAll();
    }
}
