package com.footballleague.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TeamRequest(

        @NotBlank(message = "Takım adı boş olamaz")
        @Size(max = 100, message = "Takım adı en fazla 100 karakter olabilir")
        String name,

        @NotNull(message = "Kuruluş yılı zorunludur")
        @Min(value = 1850, message = "Geçerli bir kuruluş yılı giriniz")
        Integer foundedYear,

        @NotBlank(message = "Renkler boş olamaz")
        @Size(max = 100, message = "Renkler en fazla 100 karakter olabilir")
        String colors
) {
}
