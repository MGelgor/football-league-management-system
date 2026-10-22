package com.footballleague.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(
        @NotBlank(message = "Oda adı boş olamaz") @Size(max = 60, message = "Oda adı en fazla 60 karakter") String roomName,
        @NotBlank(message = "Takma ad boş olamaz") @Size(max = 30, message = "Takma ad en fazla 30 karakter") String nickname
) {
}
