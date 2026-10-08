package com.footballleague.dto;

import java.util.List;

import com.footballleague.entity.Formation;
import com.footballleague.entity.PlayStyle;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LineupRequest(
        @NotNull(message = "Diziliş seçilmeli") Formation formation,
        @NotNull(message = "Oyun stili seçilmeli") PlayStyle playStyle,
        @NotNull @Size(min = 11, max = 11, message = "İlk 11'de tam 11 oyuncu olmalı") List<Long> starterIds,
        @NotNull(message = "Kaptan seçilmeli") Long captainId,
        @NotNull(message = "Penaltıcı seçilmeli") Long penaltyTakerId
) {
}
