package com.footballleague.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.footballleague.entity.InboxMessage;
import com.footballleague.entity.InboxMessageType;

public interface InboxMessageRepository extends JpaRepository<InboxMessage, Long> {

    @Query("""
            select m from InboxMessage m
            left join fetch m.player
            left join fetch m.team
            order by m.id desc
            """)
    List<InboxMessage> findAllNewestFirst();

    long countByReadFalse();

    boolean existsByTypeAndPlayerIdAndResolvedFalse(InboxMessageType type, Long playerId);

    List<InboxMessage> findByTypeAndResolvedFalse(InboxMessageType type);
}
