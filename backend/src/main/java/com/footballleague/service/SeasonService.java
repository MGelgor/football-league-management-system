package com.footballleague.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.footballleague.dto.SeasonResultResponse;
import com.footballleague.dto.StandingResponse;
import com.footballleague.entity.Match;
import com.footballleague.entity.MatchWeek;
import com.footballleague.exception.FixtureNotGeneratedException;
import com.footballleague.repository.MatchRepository;
import com.footballleague.repository.MatchWeekRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SeasonService {

    private final MatchWeekRepository matchWeekRepository;
    private final MatchRepository matchRepository;
    private final MatchSimulationService matchSimulationService;
    private final StandingsService standingsService;

    public SeasonResultResponse playRemainingSeason() {
        List<MatchWeek> weeks = matchWeekRepository.findAll(Sort.by("weekNumber"));
        if (weeks.isEmpty()) {
            throw new FixtureNotGeneratedException();
        }

        for (MatchWeek week : weeks) {
            boolean alreadyPlayed = matchRepository.findByMatchWeekIdOrderById(week.getId()).stream()
                    .anyMatch(Match::isPlayed);
            if (!alreadyPlayed) {
                matchSimulationService.playWeek(week.getWeekNumber());
            }
        }

        List<StandingResponse> finalStandings = standingsService.getStandings();
        StandingResponse champion = finalStandings.get(0);

        return new SeasonResultResponse(champion.teamName(), champion.points(), finalStandings);
    }
}
