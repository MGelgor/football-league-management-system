package com.footballleague.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.footballleague.entity.MatchTeamStats;

public interface MatchTeamStatsRepository extends JpaRepository<MatchTeamStats, Long> {

    List<MatchTeamStats> findByMatchId(Long matchId);

    /** Takımın sezondaki lig maçı istatistikleri, hafta sırasıyla (takım istatistikleri, güç geçmişi). */
    @Query("""
            select s from MatchTeamStats s
            join fetch s.match m
            join fetch m.matchWeek w
            where s.team.id = :teamId and w.season.id = :seasonId
              and w.competition in (com.footballleague.entity.Competition.LEAGUE,
                com.footballleague.entity.Competition.SECOND_LEAGUE)
            order by w.weekNumber asc
            """)
    List<MatchTeamStats> findLeagueByTeamAndSeason(@Param("teamId") Long teamId, @Param("seasonId") Long seasonId);

    @Modifying
    @Query("""
            delete from MatchTeamStats s where s.match.id in
            (select m.id from Match m where m.matchWeek.season.id = :seasonId)
            """)
    void deleteBySeasonId(@Param("seasonId") Long seasonId);
}
