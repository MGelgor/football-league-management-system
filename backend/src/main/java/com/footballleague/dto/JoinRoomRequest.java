package com.footballleague.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinRoomRequest(
        @NotBlank(message = "Takma ad boş olamaz") @Size(max = 30, message = "Takma ad en fazla 30 karakter") String nickname
) {
}
