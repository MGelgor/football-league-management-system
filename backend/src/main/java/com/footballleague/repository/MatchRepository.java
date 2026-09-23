package com.footballleague.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.footballleague.entity.Match;

public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByMatchWeekIdOrderById(Long matchWeekId);

    @Query("""
            select m from Match m
            join fetch m.homeTeam
            join fetch m.awayTeam
            join fetch m.matchWeek
            order by m.matchWeek.weekNumber asc, m.id asc
            """)
    List<Match> findAllWithTeamsOrderByWeek();
}
