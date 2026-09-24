package com.footballleague.dto;

public record TeamResponse(
        Long id,
        String name,
        Integer foundedYear,
        String colors,
        String logoUrl,
        Integer strength,
        Integer morale
) {
}
