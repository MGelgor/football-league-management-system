package com.footballleague.dto;

import com.footballleague.entity.Position;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlayerRequest(

        @NotBlank(message = "Oyuncu adı boş olamaz")
        @Size(max = 100, message = "Oyuncu adı en fazla 100 karakter olabilir")
        String name,

        @NotNull(message = "Mevki zorunludur")
        Position position,

        @NotNull(message = "Forma numarası zorunludur")
        @Min(value = 1, message = "Forma numarası 1-99 arasında olmalı")
        @Max(value = 99, message = "Forma numarası 1-99 arasında olmalı")
        Integer shirtNumber,

        @NotNull(message = "Oyuncu gücü zorunludur")
        @Min(value = 1, message = "Oyuncu gücü 1-100 arasında olmalı")
        @Max(value = 100, message = "Oyuncu gücü 1-100 arasında olmalı")
        Integer strength,

        @NotNull(message = "Yaş zorunludur")
        @Min(value = 16, message = "Yaş 16-45 arasında olmalı")
        @Max(value = 45, message = "Yaş 16-45 arasında olmalı")
        Integer age
) {
}
