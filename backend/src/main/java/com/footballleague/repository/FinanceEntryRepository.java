package com.footballleague.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.footballleague.entity.FinanceEntry;

public interface FinanceEntryRepository extends JpaRepository<FinanceEntry, Long> {

    List<FinanceEntry> findByTeamIdAndSeasonIdOrderByIdDesc(Long teamId, Long seasonId);

    @Query("select f from FinanceEntry f join fetch f.team where f.season.id = :seasonId")
    List<FinanceEntry> findBySeasonIdWithTeam(@Param("seasonId") Long seasonId);
}
