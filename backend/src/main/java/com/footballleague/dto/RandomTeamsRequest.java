package com.footballleague.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RandomTeamsRequest(

        @NotNull(message = "Takım sayısı zorunludur")
        @Min(value = 1, message = "Takım sayısı 1-50 arasında olmalı")
        @Max(value = 50, message = "Takım sayısı 1-50 arasında olmalı")
        Integer count
) {
}
