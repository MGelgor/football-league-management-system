package com.footballleague.service;

import java.time.Year;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.footballleague.dto.TeamRequest;
import com.footballleague.dto.TeamResponse;
import com.footballleague.entity.Team;
import com.footballleague.exception.DuplicateTeamNameException;
import com.footballleague.exception.TeamNotFoundException;
import com.footballleague.repository.TeamRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamService {

    private final TeamRepository teamRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<TeamResponse> getAllTeams() {
        return teamRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TeamResponse getTeam(Long id) {
        return toResponse(findTeamOrThrow(id));
    }

    public TeamResponse createTeam(TeamRequest request) {
        validateFoundedYear(request.foundedYear());
        if (teamRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateTeamNameException(request.name());
        }

        Team team = Team.builder()
                .name(request.name())
                .foundedYear(request.foundedYear())
                .colors(request.colors())
                .build();

        return toResponse(teamRepository.save(team));
    }

    public TeamResponse updateTeam(Long id, TeamRequest request) {
        Team team = findTeamOrThrow(id);
        validateFoundedYear(request.foundedYear());

        teamRepository.findByNameIgnoreCase(request.name())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateTeamNameException(request.name());
                });

        team.setName(request.name());
        team.setFoundedYear(request.foundedYear());
        team.setColors(request.colors());

        return toResponse(teamRepository.save(team));
    }

    public void deleteTeam(Long id) {
        Team team = findTeamOrThrow(id);
        teamRepository.delete(team);
    }

    public TeamResponse updateLogo(Long id, MultipartFile file) {
        Team team = findTeamOrThrow(id);
        team.setLogoPath(fileStorageService.storeLogo(file));
        return toResponse(teamRepository.save(team));
    }

    private Team findTeamOrThrow(Long id) {
        return teamRepository.findById(id).orElseThrow(() -> new TeamNotFoundException(id));
    }

    private void validateFoundedYear(Integer foundedYear) {
        int currentYear = Year.now().getValue();
        if (foundedYear > currentYear) {
            throw new IllegalArgumentException("Kuruluş yılı gelecekte olamaz");
        }
    }

    private TeamResponse toResponse(Team team) {
        String logoUrl = team.getLogoPath() != null ? "/uploads/" + team.getLogoPath() : null;
        return new TeamResponse(team.getId(), team.getName(), team.getFoundedYear(), team.getColors(), logoUrl);
    }
}
