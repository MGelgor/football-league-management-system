package com.footballleague.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.footballleague.entity.Match;

public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByMatchWeekIdOrderById(Long matchWeekId);

    /** Sezonun lig maçları (kupa hariç), hafta sırasıyla; puan durumu ve fikstür bunu kullanır. */
    @Query("""
            select m from Match m
            join fetch m.homeTeam
            join fetch m.awayTeam
            join fetch m.matchWeek w
            where w.season.id = :seasonId and w.competition = com.footballleague.entity.Competition.LEAGUE
            order by w.weekNumber asc, m.id asc
            """)
    List<Match> findLeagueMatchesBySeason(@Param("seasonId") Long seasonId);

    @Query("""
            select m from Match m
            join fetch m.homeTeam
            join fetch m.awayTeam
            join fetch m.matchWeek w
            where w.season.id = :seasonId and w.competition = com.footballleague.entity.Competition.CUP
            order by w.weekNumber asc, m.id asc
            """)
    List<Match> findCupMatchesBySeason(@Param("seasonId") Long seasonId);

    /** Tüm sezonların oynanmış lig maçları (rekorlar için). */
    @Query("""
            select m from Match m
            join fetch m.homeTeam
            join fetch m.awayTeam
            join fetch m.matchWeek w
            join fetch w.season s
            where w.competition = com.footballleague.entity.Competition.LEAGUE and m.homeScore is not null
            order by s.seasonNumber asc, w.weekNumber asc, m.id asc
            """)
    List<Match> findAllPlayedLeagueMatches();

    /** İki takım arasındaki tüm oynanmış maçlar (lig + kupa, tüm sezonlar). */
    @Query("""
            select m from Match m
            join fetch m.homeTeam h
            join fetch m.awayTeam a
            join fetch m.matchWeek w
            join fetch w.season s
            where m.homeScore is not null
              and ((h.id = :teamA and a.id = :teamB) or (h.id = :teamB and a.id = :teamA))
            order by s.seasonNumber asc, w.weekNumber asc
            """)
    List<Match> findHeadToHead(@Param("teamA") Long teamA, @Param("teamB") Long teamB);

    @Query("""
            select m from Match m
            join fetch m.homeTeam
            join fetch m.awayTeam
            join fetch m.matchWeek w
            join fetch w.season
            where m.id = :id
            """)
    Optional<Match> findDetailById(@Param("id") Long id);

    @Query("""
            select min(m.matchWeek.weekNumber) from Match m
            where m.matchWeek.season.id = :seasonId and m.homeScore is null
              and m.matchWeek.competition = com.footballleague.entity.Competition.LEAGUE
            """)
    Optional<Integer> findFirstUnplayedWeekNumber(@Param("seasonId") Long seasonId);

    @Query("""
            select count(m) from Match m
            where m.matchWeek.season.id = :seasonId
              and m.matchWeek.competition = com.footballleague.entity.Competition.LEAGUE
            """)
    long countLeagueMatches(@Param("seasonId") Long seasonId);

    @Query("""
            select count(m) from Match m
            where m.matchWeek.season.id = :seasonId and m.homeScore is not null
              and m.matchWeek.competition = com.footballleague.entity.Competition.LEAGUE
            """)
    long countPlayedLeagueMatches(@Param("seasonId") Long seasonId);

    boolean existsByHomeTeamIdOrAwayTeamId(Long homeTeamId, Long awayTeamId);

    @Modifying
    @Query("delete from Match m where m.matchWeek.id in (select w.id from MatchWeek w where w.season.id = :seasonId)")
    void deleteBySeasonId(@Param("seasonId") Long seasonId);
}
