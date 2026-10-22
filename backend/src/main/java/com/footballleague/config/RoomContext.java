package com.footballleague.config;

import java.time.Instant;
import java.util.function.Supplier;

/**
 * İsteğin hangi odada çalıştığı (thread'e bağlı). RoomFilter istek başında doldurur, sonunda temizler;
 * RoomAwareDataSource bağlantının şemasını buna göre ayarlar. Boşsa varsayılan (tek kişilik) lig kullanılır.
 */
public final class RoomContext {

    /**
     * memberId: isteği yapan üye (zamanlayıcının işinde null). lineupLockAt: bu andan sonra sıradaki maç
     * gününün kadrosu / taktiği değiştirilemez (sezonda maç saatinden 5 dk önce; başka zaman null).
     * liveWeek / liveStartedAt: son oynanan maç günü ve canlı yayının başladığı an (yayın Room.LIVE_DURATION sürer).
     */
    public record Current(Long roomId, String code, String schema, Long memberId, boolean owner,
            Instant lineupLockAt, Integer liveWeek, Instant liveStartedAt) {

        /** Zamanlayıcının ve oda kurulumunun işleri için (üye yok). */
        public static Current system(Long roomId, String code, String schema) {
            return new Current(roomId, code, schema, null, true, null, null, null);
        }
    }

    private static final ThreadLocal<Current> CURRENT = new ThreadLocal<>();

    private RoomContext() {
    }

    public static Current get() {
        return CURRENT.get();
    }

    /** Odanın şeması; oda yoksa null. */
    public static String schema() {
        Current current = CURRENT.get();
        return current == null ? null : current.schema();
    }

    public static void set(Current current) {
        CURRENT.set(current);
    }

    public static void clear() {
        CURRENT.remove();
    }

    /** İşi verilen odada çalıştırır, sonra önceki odaya döner. Açık bir transaction'ın içinden çağrılmamalı. */
    public static <T> T call(Current room, Supplier<T> work) {
        Current previous = CURRENT.get();
        CURRENT.set(room);
        try {
            return work.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}
