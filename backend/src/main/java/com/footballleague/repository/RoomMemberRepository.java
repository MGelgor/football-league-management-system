package com.footballleague.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.footballleague.entity.RoomMember;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {

    Optional<RoomMember> findByRoomCodeAndTokenHash(String code, String tokenHash);

    List<RoomMember> findByRoomIdOrderByJoinedAtAsc(Long roomId);

    long countByRoomId(Long roomId);

    boolean existsByRoomIdAndNicknameIgnoreCase(Long roomId, String nickname);

    void deleteByRoomId(Long roomId);
}
