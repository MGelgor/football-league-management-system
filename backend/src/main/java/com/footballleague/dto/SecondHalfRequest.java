package com.footballleague.dto;

import java.util.List;

import com.footballleague.entity.PlayStyle;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Devre arası kararları: değişiklikler, ikinci yarı stili ve soyunma odası konuşması. */
public record SecondHalfRequest(
        @Valid List<Substitution> substitutions,
        @NotNull(message = "Oyun stili seçilmeli") PlayStyle playStyle,
        @NotNull(message = "Konuşma seçilmeli") TeamTalk talk
) {

    public record Substitution(@NotNull Long outId, @NotNull Long inId) {
    }

    /** CALM: önde iken rakibin golünü azaltır; MOTIVATE: geride / berabereyken golü artırır; CRITICIZE: riskli. */
    public enum TeamTalk {
        NONE,
        CALM,
        MOTIVATE,
        CRITICIZE
    }
}
