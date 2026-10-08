package com.footballleague.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.footballleague.entity.Competition;
import com.footballleague.entity.MatchAppearance;

public interface MatchAppearanceRepository extends JpaRepository<MatchAppearance, Long> {

    @Query("""
            select a from MatchAppearance a
            join fetch a.player
            left join fetch a.replacedPlayer
            where a.match.id = :matchId
            order by a.id asc
            """)
    List<MatchAppearance> findByMatchIdWithPlayers(@Param("matchId") Long matchId);

    @Query("""
            select a from MatchAppearance a
            where a.team.id = :teamId and a.match.matchWeek.season.id = :seasonId
              and a.match.matchWeek.competition in (com.footballleague.entity.Competition.LEAGUE,
                com.footballleague.entity.Competition.SECOND_LEAGUE)
            """)
    List<MatchAppearance> findLeagueByTeamAndSeason(@Param("teamId") Long teamId, @Param("seasonId") Long seasonId);

    List<MatchAppearance> findByTeamId(Long teamId);

    /** Sezonun lig maç kayıtları, oyuncu ve o maçtaki takımıyla (oyuncu istatistikleri, sezon sonu gelişimi). */
    @Query("""
            select a from MatchAppearance a
            join fetch a.player p
            join fetch a.team
            where a.match.matchWeek.season.id = :seasonId
              and a.match.matchWeek.competition = com.footballleague.entity.Competition.LEAGUE
            """)
    List<MatchAppearance> findLeagueBySeasonWithPlayers(@Param("seasonId") Long seasonId);

    /** Bir ligin (LEAGUE / SECOND_LEAGUE) sezon kayıtları; oyuncu ve o maçtaki takımıyla. */
    @Query("""
            select a from MatchAppearance a
            join fetch a.player p
            join fetch a.team
            where a.match.matchWeek.season.id = :seasonId and a.match.matchWeek.competition = :competition
            """)
    List<MatchAppearance> findBySeasonAndCompetitionWithPlayers(@Param("seasonId") Long seasonId,
            @Param("competition") Competition competition);

    @Query("""
            select a from MatchAppearance a
            join fetch a.match m
            join fetch m.matchWeek w
            join fetch w.season
            where a.player.id = :playerId
            """)
    List<MatchAppearance> findByPlayerIdWithMatch(@Param("playerId") Long playerId);

    @Modifying
    @Query("""
            delete from MatchAppearance a where a.match.id in
            (select m.id from Match m where m.matchWeek.season.id = :seasonId)
            """)
    void deleteBySeasonId(@Param("seasonId") Long seasonId);
}
