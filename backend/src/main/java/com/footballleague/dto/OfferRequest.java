package com.footballleague.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OfferRequest(
        @NotNull(message = "Oyuncu seçilmeli") Long playerId,
        @NotNull(message = "Alıcı takım seçilmeli") Long buyerTeamId,
        @NotNull(message = "Teklif bedeli girilmeli") @Min(value = 0, message = "Teklif negatif olamaz") Long fee
) {
}
