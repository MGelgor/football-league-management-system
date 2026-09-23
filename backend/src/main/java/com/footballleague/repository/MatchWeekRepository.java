package com.footballleague.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.footballleague.entity.MatchWeek;

public interface MatchWeekRepository extends JpaRepository<MatchWeek, Long> {
}
