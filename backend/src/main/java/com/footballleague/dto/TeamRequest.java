package com.footballleague.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TeamRequest(

        @NotBlank(message = "Takım adı boş olamaz")
        String name,

        @NotNull(message = "Kuruluş yılı zorunludur")
        @Min(value = 1850, message = "Geçerli bir kuruluş yılı giriniz")
        Integer foundedYear,

        @NotBlank(message = "Renkler boş olamaz")
        String colors
) {
}
