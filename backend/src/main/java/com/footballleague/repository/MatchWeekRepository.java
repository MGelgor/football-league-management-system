package com.footballleague.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.footballleague.entity.MatchWeek;

public interface MatchWeekRepository extends JpaRepository<MatchWeek, Long> {

    Optional<MatchWeek> findByWeekNumber(Integer weekNumber);
}
