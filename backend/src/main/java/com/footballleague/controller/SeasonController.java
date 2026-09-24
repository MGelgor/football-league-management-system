package com.footballleague.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.footballleague.dto.SeasonResultResponse;
import com.footballleague.service.SeasonService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/season")
@RequiredArgsConstructor
@Tag(name = "Season", description = "Sezonu otomatik tamamlama ve şampiyon belirleme")
public class SeasonController {

    private final SeasonService seasonService;

    @PostMapping("/play-all")
    public SeasonResultResponse playAll() {
        return seasonService.playRemainingSeason();
    }
}
