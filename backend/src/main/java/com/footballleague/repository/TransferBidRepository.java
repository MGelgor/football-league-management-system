package com.footballleague.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.footballleague.entity.BidStatus;
import com.footballleague.entity.TransferBid;

public interface TransferBidRepository extends JpaRepository<TransferBid, Long> {

    /** Bekleyen teklifler: önce en yüksek bedel, eşitlikte önce verilen. */
    @Query("""
            select b from TransferBid b join fetch b.player join fetch b.team
            where b.status = com.footballleague.entity.BidStatus.PENDING
            order by b.fee desc, b.id asc
            """)
    List<TransferBid> findPending();

    @Query("""
            select b from TransferBid b join fetch b.player p left join fetch p.team
            where b.team.id = :teamId and b.seasonNumber = :seasonNumber
            order by b.id desc
            """)
    List<TransferBid> findByTeamAndSeason(@Param("teamId") Long teamId, @Param("seasonNumber") int seasonNumber);

    Optional<TransferBid> findByTeamIdAndPlayerIdAndStatus(Long teamId, Long playerId, BidStatus status);
}
