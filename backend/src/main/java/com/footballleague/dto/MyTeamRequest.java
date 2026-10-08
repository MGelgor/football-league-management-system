package com.footballleague.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MyTeamRequest(
        @NotBlank(message = "Menajer adı boş olamaz") @Size(max = 60, message = "Ad en fazla 60 karakter") String managerName,
        @NotNull(message = "Takım seçilmeli") Long teamId
) {
}
