package com.footballleague.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.footballleague.entity.RoomMember;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {

    /** Üye ve odası tek sorguda (transaction dışında da oda alanları okunabilsin). */
    @Query("select m from RoomMember m join fetch m.room r where r.code = :code and m.tokenHash = :tokenHash")
    Optional<RoomMember> findWithRoom(@Param("code") String code, @Param("tokenHash") String tokenHash);

    List<RoomMember> findByRoomIdOrderByJoinedAtAsc(Long roomId);

    long countByRoomId(Long roomId);

    boolean existsByRoomIdAndNicknameIgnoreCase(Long roomId, String nickname);

    void deleteByRoomId(Long roomId);

    boolean existsByRoomIdAndReadyFalse(Long roomId);

    @Modifying
    @Transactional
    @Query("update RoomMember m set m.ready = false where m.room.id = :roomId")
    void resetReady(@Param("roomId") Long roomId);
}
