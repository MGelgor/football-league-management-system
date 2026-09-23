package com.footballleague.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.footballleague.dto.MatchWeekResponse;
import com.footballleague.service.FixtureService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/fixtures")
@RequiredArgsConstructor
@Tag(name = "Fixtures", description = "Fikstür oluşturma ve görüntüleme")
public class FixtureController {

    private final FixtureService fixtureService;

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public List<MatchWeekResponse> generateFixture() {
        return fixtureService.generateFixture();
    }

    @GetMapping
    public List<MatchWeekResponse> getFixture() {
        return fixtureService.getFixture();
    }
}
