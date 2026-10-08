package com.footballleague.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ContractOfferRequest(
        @NotNull(message = "Maaş girilmeli") @Min(value = 0, message = "Maaş negatif olamaz") Long weeklyWage,
        @NotNull @Min(value = 1, message = "En az 1 sezon") @Max(value = 4, message = "En fazla 4 sezon") Integer seasons
) {
}
