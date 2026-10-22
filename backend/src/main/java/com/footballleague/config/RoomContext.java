package com.footballleague.config;

import java.util.function.Supplier;

/**
 * İsteğin hangi odada çalıştığı (thread'e bağlı). RoomFilter istek başında doldurur, sonunda temizler;
 * RoomAwareDataSource bağlantının şemasını buna göre ayarlar. Boşsa varsayılan (tek kişilik) lig kullanılır.
 */
public final class RoomContext {

    public record Current(Long roomId, String code, String schema, Long memberId, boolean owner) {
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
