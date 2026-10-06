package com.footballleague.dto;

/**
 * Tek bir tarihsel rekor. holder: rekor sahibi (takım, oyuncu ya da maç), value: rekor değeri,
 * detail: ek bilgi (sezon, hafta...). teamId / playerId / matchId bağlantı için, ilgisizse null.
 */
public record RecordResponse(
        String title,
        String holder,
        String value,
        String detail,
        Long teamId,
        Long playerId,
        Long matchId
) {
}
