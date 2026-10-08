package com.footballleague.dto;

import com.footballleague.entity.Formation;
import com.footballleague.entity.PlayStyle;

import jakarta.validation.constraints.NotNull;

public record TacticsRequest(
        @NotNull(message = "Diziliş seçilmeli") Formation formation,
        @NotNull(message = "Oyun stili seçilmeli") PlayStyle playStyle
) {
}
