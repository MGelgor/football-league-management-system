package com.footballleague.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RoomSettingsRequest(
        @NotBlank @Pattern(regexp = "([01]\\d|2[0-3]):[0-5]\\d", message = "Maç saati SS:DD biçiminde olmalı") String matchTime,
        @NotBlank(message = "Saat dilimi boş olamaz") String timeZone,
        @Min(value = 1, message = "Transfer penceresi en az 1 saat") @Max(value = 72, message = "Transfer penceresi en fazla 72 saat") int transferHours
) {
}
