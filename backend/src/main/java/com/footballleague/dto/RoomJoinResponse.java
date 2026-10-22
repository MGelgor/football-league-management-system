package com.footballleague.dto;

/** Oda kurulunca / katılınca döner. Token yalnızca burada bir kez görünür; istemci saklar. */
public record RoomJoinResponse(RoomResponse room, String token) {
}
