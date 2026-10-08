package com.footballleague.dto;

import jakarta.validation.constraints.NotNull;

public record SignRequest(@NotNull(message = "Takım seçilmeli") Long teamId) {
}
