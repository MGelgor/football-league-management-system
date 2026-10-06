package com.footballleague.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.footballleague.entity.MatchEvent;

/** "Sezon" sorguları yalnızca lig maçlarını sayar; kariyer sorguları lig + kupa. */
public interface MatchEventRepository extends JpaRepository<MatchEvent, Long> {

    @Query("""
            select e from MatchEvent e
            join fetch e.player
            left join fetch e.assistPlayer
            where e.match.id = :matchId
            order by e.minute asc, e.id asc
            """)
    List<MatchEvent> findByMatchIdWithPlayers(@Param("matchId") Long matchId);

    @Query("""
            select e from MatchEvent e
            where e.team.id = :teamId and e.match.matchWeek.season.id = :seasonId
              and e.match.matchWeek.competition = com.footballleague.entity.Competition.LEAGUE
            """)
    List<MatchEvent> findLeagueEventsByTeamAndSeason(@Param("teamId") Long teamId, @Param("seasonId") Long seasonId);

    /** Takımın tüm sezonlardaki olayları (oyuncu kariyer istatistikleri için). */
    List<MatchEvent> findByTeamId(Long teamId);

    /** Bir sezonun lig olayları; oyuncular ve takımları tek sorguda yüklenir (oyuncu istatistik tablosu). */
    @Query("""
            select e from MatchEvent e
            join fetch e.player p
            join fetch p.team
            left join fetch e.assistPlayer ap
            left join fetch ap.team
            where e.match.matchWeek.season.id = :seasonId
              and e.match.matchWeek.competition = com.footballleague.entity.Competition.LEAGUE
            """)
    List<MatchEvent> findLeagueEventsBySeasonWithPlayers(@Param("seasonId") Long seasonId);

    /** Oyuncu sayfası: oyuncunun yer aldığı (gol, asist, kart, sakatlık) tüm olaylar, maç bilgisiyle. */
    @Query("""
            select e from MatchEvent e
            join fetch e.match m
            join fetch m.homeTeam
            join fetch m.awayTeam
            join fetch m.matchWeek w
            join fetch w.season s
            join fetch e.player p
            left join fetch e.assistPlayer ap
            where p.id = :playerId or ap.id = :playerId
            order by s.seasonNumber asc, w.weekNumber asc, e.minute asc
            """)
    List<MatchEvent> findByPlayerInvolved(@Param("playerId") Long playerId);

    /** Tüm sezonların lig golleri (gol krallığı rekorları için). */
    @Query("""
            select e from MatchEvent e
            join fetch e.player p
            join fetch p.team
            join fetch e.match m
            join fetch m.matchWeek w
            join fetch w.season
            where e.type = com.footballleague.entity.MatchEventType.GOAL
              and w.competition = com.footballleague.entity.Competition.LEAGUE
            """)
    List<MatchEvent> findAllLeagueGoals();

    @Modifying
    @Query("""
            delete from MatchEvent e where e.match.id in
            (select m.id from Match m where m.matchWeek.season.id = :seasonId)
            """)
    void deleteBySeasonId(@Param("seasonId") Long seasonId);
}
