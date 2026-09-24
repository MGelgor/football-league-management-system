package com.footballleague.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.footballleague.dto.StandingResponse;

/**
 * Oynanan maç sonuçlarından puan durumunu hesaplar.
 * Sıralama kuralı: Puan -> Averaj -> Atılan gol (spesifikasyon Bölüm 8).
 */
@Component
public class StandingsCalculator {

    public List<StandingResponse> calculate(List<TeamInfo> teams, List<MatchResult> playedMatches) {
        Map<Long, Standing> standingByTeam = new LinkedHashMap<>();
        teams.forEach(team -> standingByTeam.put(team.id(), new Standing(team.id(), team.name())));

        for (MatchResult result : playedMatches) {
            Standing home = standingByTeam.get(result.homeTeamId());
            Standing away = standingByTeam.get(result.awayTeamId());
            home.recordMatch(result.homeGoals(), result.awayGoals());
            away.recordMatch(result.awayGoals(), result.homeGoals());
        }

        List<Standing> sorted = standingByTeam.values().stream()
                .sorted(Comparator.comparingInt(Standing::points).reversed()
                        .thenComparing(Comparator.comparingInt(Standing::goalDifference).reversed())
                        .thenComparing(Comparator.comparingInt(Standing::goalsFor).reversed()))
                .toList();

        List<StandingResponse> responses = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            responses.add(sorted.get(i).toResponse(i + 1));
        }
        return responses;
    }

    public record TeamInfo(Long id, String name) {
    }

    public record MatchResult(Long homeTeamId, int homeGoals, Long awayTeamId, int awayGoals) {
    }

    private static final class Standing {
        private final Long teamId;
        private final String teamName;
        private int played;
        private int won;
        private int drawn;
        private int lost;
        private int goalsFor;
        private int goalsAgainst;

        private Standing(Long teamId, String teamName) {
            this.teamId = teamId;
            this.teamName = teamName;
        }

        private void recordMatch(int goalsScored, int goalsConceded) {
            played++;
            goalsFor += goalsScored;
            goalsAgainst += goalsConceded;
            if (goalsScored > goalsConceded) {
                won++;
            } else if (goalsScored < goalsConceded) {
                lost++;
            } else {
                drawn++;
            }
        }

        private int points() {
            return won * 3 + drawn;
        }

        private int goalDifference() {
            return goalsFor - goalsAgainst;
        }

        private int goalsFor() {
            return goalsFor;
        }

        private StandingResponse toResponse(int rank) {
            return new StandingResponse(rank, teamId, teamName, played, won, drawn, lost, goalsFor, goalsAgainst,
                    goalDifference(), points());
        }
    }
}
