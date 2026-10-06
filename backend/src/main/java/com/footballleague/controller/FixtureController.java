package com.footballleague.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.footballleague.dto.MatchWeekResponse;
import com.footballleague.service.FixtureService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/fixtures")
@RequiredArgsConstructor
@Tag(name = "Fixtures", description = "Yeni sezon fikstürü oluşturma, görüntüleme ve devam eden sezonu sıfırlama")
public class FixtureController {

    private final FixtureService fixtureService;

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public List<MatchWeekResponse> generateFixture() {
        return fixtureService.generateFixture();
    }

    @GetMapping
    public List<MatchWeekResponse> getFixture(@RequestParam(required = false) Long seasonId) {
        return fixtureService.getFixture(seasonId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetFixture() {
        fixtureService.resetFixture();
    }
}
