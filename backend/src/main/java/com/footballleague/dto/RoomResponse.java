package com.footballleague.dto;

import java.time.Instant;
import java.util.List;

import com.footballleague.entity.RoomStatus;

/**
 * Oda bilgisi; you = isteği yapan üyenin id'si. nextEventAt: sıradaki maç günü (sezonda) ya da transfer
 * penceresinin kapanışı; nextMatchDay: o gün ne oynanacak (ör. "5. hafta").
 */
public record RoomResponse(String code, String name, Instant createdAt, int maxMembers, List<Member> members,
        Long you, RoomStatus status, String matchTime, String timeZone, int transferHours, Instant nextEventAt,
        String nextMatchDay, Integer seasonNumber) {

    public record Member(Long id, String nickname, boolean owner, Instant joinedAt, boolean ready) {
    }
}
