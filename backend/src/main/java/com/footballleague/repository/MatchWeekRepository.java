package com.footballleague.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.footballleague.entity.Competition;
import com.footballleague.entity.MatchWeek;

public interface MatchWeekRepository extends JpaRepository<MatchWeek, Long> {

    Optional<MatchWeek> findBySeasonIdAndWeekNumber(Long seasonId, Integer weekNumber);

    List<MatchWeek> findBySeasonIdAndCompetitionOrderByWeekNumber(Long seasonId, Competition competition);

    @Modifying
    @Query("delete from MatchWeek w where w.season.id = :seasonId")
    void deleteBySeasonId(@Param("seasonId") Long seasonId);
}
