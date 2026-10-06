package com.footballleague.dto;

import java.util.List;

import jakarta.validation.Valid;
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
        String colors,

        // İsteğe bağlı, yalnızca takım oluştururken kullanılır; eksik mevkiler rastgele oyuncularla tamamlanır
        @Size(max = 30, message = "En fazla 30 oyuncu eklenebilir")
        List<@Valid PlayerRequest> players
) {

    public TeamRequest(String name, Integer foundedYear, String colors) {
        this(name, foundedYear, colors, null);
    }
}
