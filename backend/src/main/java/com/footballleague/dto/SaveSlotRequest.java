package com.footballleague.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SaveSlotRequest(
        @NotBlank(message = "Kayıt adı boş olamaz")
        @Pattern(regexp = "[\\p{L}0-9 _-]{1,40}", message = "Kayıt adı en fazla 40 karakter; harf, rakam, boşluk, - ve _ olabilir")
        String name
) {
}
