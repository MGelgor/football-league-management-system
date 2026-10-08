package com.footballleague.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.footballleague.entity.MatchLineup;

public interface MatchLineupRepository extends JpaRepository<MatchLineup, Long> {

    Optional<MatchLineup> findByMatchIdAndTeamId(Long matchId, Long teamId);

    List<MatchLineup> findByMatchIdIn(Collection<Long> matchIds);

    List<MatchLineup> findByMatchId(Long matchId);

    @Modifying
    @Query("""
            delete from MatchLineup l where l.match.id in
            (select m.id from Match m where m.matchWeek.season.id = :seasonId)
            """)
    void deleteBySeasonId(@Param("seasonId") Long seasonId);
}
