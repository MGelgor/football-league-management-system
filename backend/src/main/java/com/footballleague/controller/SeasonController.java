package com.footballleague.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.footballleague.dto.SeasonResponse;
import com.footballleague.dto.SeasonResultResponse;
import com.footballleague.service.SeasonService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/seasons")
@RequiredArgsConstructor
@Tag(name = "Season", description = "Sezon listesi, sezonu otomatik tamamlama ve şampiyon belirleme")
public class SeasonController {

    private final SeasonService seasonService;

    @GetMapping
    public List<SeasonResponse> getSeasons() {
        return seasonService.getSeasons();
    }

    @PostMapping("/play-all")
    public SeasonResultResponse playAll() {
        return seasonService.playRemainingSeason();
    }
}
