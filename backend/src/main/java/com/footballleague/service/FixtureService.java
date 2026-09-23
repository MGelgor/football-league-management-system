package com.footballleague.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.footballleague.dto.MatchResponse;
import com.footballleague.dto.MatchWeekResponse;
import com.footballleague.entity.Match;
import com.footballleague.entity.MatchWeek;
import com.footballleague.entity.Team;
import com.footballleague.exception.FixtureAlreadyGeneratedException;
import com.footballleague.repository.MatchRepository;
import com.footballleague.repository.MatchWeekRepository;
import com.footballleague.repository.TeamRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class FixtureService {

    private static final int MIN_TEAM_COUNT = 18;

    private final TeamRepository teamRepository;
    private final MatchWeekRepository matchWeekRepository;
    private final MatchRepository matchRepository;
    private final RoundRobinScheduler roundRobinScheduler;

    public List<MatchWeekResponse> generateFixture() {
        if (matchRepository.count() > 0) {
            throw new FixtureAlreadyGeneratedException();
        }

        List<Team> teams = teamRepository.findAll();
        validateTeamCount(teams.size());
        Map<Long, Team> teamById = new LinkedHashMap<>();
        teams.forEach(team -> teamById.put(team.getId(), team));

        List<List<RoundRobinScheduler.Pairing>> firstLeg =
                roundRobinScheduler.generateSingleRoundRobin(new ArrayList<>(teamById.keySet()));

        List<MatchWeek> weeksToSave = new ArrayList<>();
        List<Match> matchesToSave = new ArrayList<>();
        int weekNumber = 1;

        for (List<RoundRobinScheduler.Pairing> weekPairings : firstLeg) {
            weekNumber = buildWeek(weekNumber, weekPairings, teamById, false, weeksToSave, matchesToSave);
        }
        for (List<RoundRobinScheduler.Pairing> weekPairings : firstLeg) {
            weekNumber = buildWeek(weekNumber, weekPairings, teamById, true, weeksToSave, matchesToSave);
        }

        matchWeekRepository.saveAll(weeksToSave);
        matchRepository.saveAll(matchesToSave);

        return getFixture();
    }

    @Transactional(readOnly = true)
    public List<MatchWeekResponse> getFixture() {
        List<Match> matches = matchRepository.findAllWithTeamsOrderByWeek();

        Map<Integer, List<MatchResponse>> matchesByWeek = new LinkedHashMap<>();
        for (Match match : matches) {
            matchesByWeek
                    .computeIfAbsent(match.getMatchWeek().getWeekNumber(), key -> new ArrayList<>())
                    .add(toMatchResponse(match));
        }

        return matchesByWeek.entrySet().stream()
                .map(entry -> new MatchWeekResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    private int buildWeek(int weekNumber, List<RoundRobinScheduler.Pairing> pairings, Map<Long, Team> teamById,
            boolean reverseVenue, List<MatchWeek> weeksToSave, List<Match> matchesToSave) {
        MatchWeek week = MatchWeek.builder().weekNumber(weekNumber).build();
        weeksToSave.add(week);

        for (RoundRobinScheduler.Pairing pairing : pairings) {
            Long homeId = reverseVenue ? pairing.awayTeamId() : pairing.homeTeamId();
            Long awayId = reverseVenue ? pairing.homeTeamId() : pairing.awayTeamId();

            matchesToSave.add(Match.builder()
                    .matchWeek(week)
                    .homeTeam(teamById.get(homeId))
                    .awayTeam(teamById.get(awayId))
                    .build());
        }
        return weekNumber + 1;
    }

    private void validateTeamCount(int count) {
        if (count < MIN_TEAM_COUNT) {
            throw new IllegalArgumentException(
                    "Fikstür oluşturmak için en az " + MIN_TEAM_COUNT + " takım gerekli (mevcut: " + count + ")");
        }
        if (count % 2 != 0) {
            throw new IllegalArgumentException("Takım sayısı çift olmalı (mevcut: " + count + ")");
        }
    }

    private MatchResponse toMatchResponse(Match match) {
        return new MatchResponse(
                match.getId(),
                match.getHomeTeam().getId(),
                match.getHomeTeam().getName(),
                match.getAwayTeam().getId(),
                match.getAwayTeam().getName(),
                match.getHomeScore(),
                match.getAwayScore(),
                match.isPlayed());
    }
}
