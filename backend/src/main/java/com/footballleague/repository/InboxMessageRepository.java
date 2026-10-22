package com.footballleague.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.footballleague.entity.InboxMessage;
import com.footballleague.entity.InboxMessageType;

/** Mesajlar alıcı menajere göre; profile null olan eski kayıtlar tek kişilik ligin menajerine aittir. */
public interface InboxMessageRepository extends JpaRepository<InboxMessage, Long> {

    @Query("""
            select m from InboxMessage m
            left join fetch m.player
            left join fetch m.team
            where m.profile.id = :profileId or m.profile is null
            order by m.id desc
            """)
    List<InboxMessage> findForProfile(@Param("profileId") Long profileId);

    @Query("select count(m) from InboxMessage m where (m.profile.id = :profileId or m.profile is null) and m.read = false")
    long countUnread(@Param("profileId") Long profileId);

    boolean existsByTypeAndPlayerIdAndResolvedFalse(InboxMessageType type, Long playerId);

    @Query("""
            select m from InboxMessage m
            where m.type = :type and m.resolved = false and (m.profile.id = :profileId or m.profile is null)
            """)
    List<InboxMessage> findOpenForProfile(@Param("type") InboxMessageType type, @Param("profileId") Long profileId);
}
