package com.footballleague.dto;

import java.util.List;
import java.util.Map;

/**
 * Tüm ligin JSON yedeği: her tablo sütun adları + satırlar (değerler sütun sırasıyla) olarak saklanır.
 * files: yüklenen logolar (yol → base64 içerik).
 */
public record LeagueSnapshot(
        int formatVersion,
        String exportedAt,
        Summary summary,
        Map<String, TableData> tables,
        Map<String, String> files
) {

    public record TableData(List<String> columns, List<List<Object>> rows) {
    }

    /** Kayıt noktası listesinde gösterilen kısa bilgi. */
    public record Summary(int teamCount, Integer seasonNumber, long playedMatches) {
    }
}
