package com.footballleague.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.footballleague.dto.HeadToHeadResponse;
import com.footballleague.dto.PlayerRequest;
import com.footballleague.dto.PlayerResponse;
import com.footballleague.dto.RandomTeamsRequest;
import com.footballleague.dto.TeamRequest;
import com.footballleague.dto.TeamResponse;
import com.footballleague.dto.TeamSeasonStatsResponse;
import com.footballleague.service.PlayerService;
import com.footballleague.service.TeamService;
import com.footballleague.service.TeamStatsService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
@Tag(name = "Teams", description = "Takım yönetimi (CRUD)")
public class TeamController {

    private final TeamService teamService;
    private final PlayerService playerService;
    private final TeamStatsService teamStatsService;

    @GetMapping
    public List<TeamResponse> getAllTeams() {
        return teamService.getAllTeams();
    }

    @GetMapping("/{id}")
    public TeamResponse getTeam(@PathVariable Long id) {
        return teamService.getTeam(id);
    }

    @GetMapping("/{id}/stats")
    public TeamSeasonStatsResponse getSeasonStats(@PathVariable Long id, @RequestParam(required = false) Long seasonId) {
        return teamStatsService.getSeasonStats(id, seasonId);
    }

    @GetMapping("/head-to-head")
    public HeadToHeadResponse headToHead(@RequestParam Long teamA, @RequestParam Long teamB) {
        return teamStatsService.headToHead(teamA, teamB);
    }

    @GetMapping("/{id}/players")
    public List<PlayerResponse> getPlayers(@PathVariable Long id) {
        return playerService.getSquad(id);
    }

    @PostMapping("/{id}/players")
    @ResponseStatus(HttpStatus.CREATED)
    public PlayerResponse addPlayer(@PathVariable Long id, @Valid @RequestBody PlayerRequest request) {
        return playerService.addPlayer(id, request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeamResponse createTeam(@Valid @RequestBody TeamRequest request) {
        return teamService.createTeam(request);
    }

    @PostMapping("/random")
    @ResponseStatus(HttpStatus.CREATED)
    public List<TeamResponse> createRandomTeams(@Valid @RequestBody RandomTeamsRequest request) {
        return teamService.createRandomTeams(request.count());
    }

    @PutMapping("/{id}")
    public TeamResponse updateTeam(@PathVariable Long id, @Valid @RequestBody TeamRequest request) {
        return teamService.updateTeam(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTeam(@PathVariable Long id) {
        teamService.deleteTeam(id);
    }

    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TeamResponse uploadLogo(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return teamService.updateLogo(id, file);
    }
}
