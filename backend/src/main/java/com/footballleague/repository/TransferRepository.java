package com.footballleague.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.footballleague.entity.Transfer;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    @Query("""
            select t from Transfer t
            join fetch t.player
            left join fetch t.fromTeam
            join fetch t.toTeam
            order by t.id desc
            """)
    List<Transfer> findAllWithTeams();

    @Query("select t.player.id from Transfer t where t.seasonNumber = :seasonNumber")
    List<Long> findPlayerIdsBySeasonNumber(@Param("seasonNumber") int seasonNumber);

    @Query("""
            select t from Transfer t
            left join fetch t.fromTeam
            join fetch t.toTeam
            where t.player.id = :playerId
            order by t.id asc
            """)
    List<Transfer> findByPlayerId(@Param("playerId") Long playerId);
}
