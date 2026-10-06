package com.footballleague.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.footballleague.dto.SeasonResponse;
import com.footballleague.dto.SeasonResultResponse;
import com.footballleague.dto.StandingResponse;
import com.footballleague.entity.Competition;
import com.footballleague.entity.Match;
import com.footballleague.entity.MatchWeek;
import com.footballleague.entity.Season;
import com.footballleague.entity.SeasonTeamChange;
import com.footballleague.entity.TeamChangeType;
import com.footballleague.exception.FixtureNotGeneratedException;
import com.footballleague.repository.MatchRepository;
import com.footballleague.repository.MatchWeekRepository;
import com.footballleague.repository.SeasonRepository;
import com.footballleague.repository.SeasonTeamChangeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SeasonService {

    private final SeasonRepository seasonRepository;
    private final MatchWeekRepository matchWeekRepository;
    private final MatchRepository matchRepository;
    private final MatchSimulationService matchSimulationService;
    private final StandingsService standingsService;
    private final SeasonTeamChangeRepository seasonTeamChangeRepository;

    /** Güncel sezonun kalan haftalarını sırayla oynatır (sezon zaten bittiyse sadece sonucu döner). */
    public SeasonResultResponse playRemainingSeason() {
        Season season = seasonRepository.findTopByOrderBySeasonNumberDesc()
                .orElseThrow(FixtureNotGeneratedException::new);

        for (MatchWeek week : matchWeekRepository.findBySeasonIdAndCompetitionOrderByWeekNumber(season.getId(),
                Competition.LEAGUE)) {
            boolean alreadyPlayed = matchRepository.findByMatchWeekIdOrderById(week.getId()).stream()
                    .anyMatch(Match::isPlayed);
            if (!alreadyPlayed) {
                matchSimulationService.playWeek(week.getWeekNumber());
            }
        }

        List<StandingResponse> finalStandings = standingsService.getStandings(season.getId());
        StandingResponse champion = finalStandings.getFirst();

        return new SeasonResultResponse(champion.teamName(), champion.points(), finalStandings);
    }

    /** En yeni sezon başta. */
    @Transactional(readOnly = true)
    public List<SeasonResponse> getSeasons() {
        Map<Long, List<SeasonTeamChange>> changesBySeason = seasonTeamChangeRepository.findAllWithTeams().stream()
                .collect(Collectors.groupingBy(change -> change.getSeason().getId()));

        return seasonRepository.findAllByOrderBySeasonNumberDesc().stream()
                .map(season -> {
                    List<SeasonTeamChange> changes = changesBySeason.getOrDefault(season.getId(), List.of());
                    return new SeasonResponse(
                            season.getId(),
                            season.getSeasonNumber(),
                            season.isFinished(),
                            season.getChampion() != null ? season.getChampion().getId() : null,
                            season.getChampion() != null ? season.getChampion().getName() : null,
                            matchRepository.countLeagueMatches(season.getId()),
                            matchRepository.countPlayedLeagueMatches(season.getId()),
                            season.getCupWinner() != null ? season.getCupWinner().getId() : null,
                            season.getCupWinner() != null ? season.getCupWinner().getName() : null,
                            teamRefs(changes, TeamChangeType.RELEGATED),
                            teamRefs(changes, TeamChangeType.PROMOTED));
                })
                .toList();
    }

    private static List<SeasonResponse.TeamRef> teamRefs(List<SeasonTeamChange> changes, TeamChangeType type) {
        return changes.stream()
                .filter(change -> change.getType() == type)
                .map(change -> new SeasonResponse.TeamRef(change.getTeam().getId(), change.getTeam().getName()))
                .toList();
    }
}
