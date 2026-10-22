package com.footballleague.dto;

import java.time.Instant;
import java.util.List;

/** Oda bilgisi; you = isteği yapan üyenin id'si. */
public record RoomResponse(String code, String name, Instant createdAt, int maxMembers, List<Member> members,
        Long you) {

    public record Member(Long id, String nickname, boolean owner, Instant joinedAt) {
    }
}
