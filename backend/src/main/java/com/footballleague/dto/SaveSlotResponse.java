package com.footballleague.dto;

public record SaveSlotResponse(String name, String savedAt, long sizeBytes, LeagueSnapshot.Summary summary) {
}
