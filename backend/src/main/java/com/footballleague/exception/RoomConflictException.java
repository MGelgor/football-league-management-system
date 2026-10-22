package com.footballleague.exception;

/** Oda dolu, takma ad kullanılıyor, oda sınırı… (409) */
public class RoomConflictException extends RuntimeException {

    public RoomConflictException(String message) {
        super(message);
    }
}
