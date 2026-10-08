package com.footballleague.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.footballleague.dto.RefereeResponse;
import com.footballleague.service.RefereeService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/referees")
@RequiredArgsConstructor
@Tag(name = "Referees", description = "Hakemler ve maç başına kart / penaltı istatistikleri")
public class RefereeController {

    private final RefereeService refereeService;

    @GetMapping
    public List<RefereeResponse> getReferees() {
        return refereeService.getReferees();
    }
}
