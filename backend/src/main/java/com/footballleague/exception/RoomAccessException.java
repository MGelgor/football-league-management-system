package com.footballleague.exception;

/** Geçersiz token ya da yalnızca oda sahibinin yapabileceği bir işlem (403). */
public class RoomAccessException extends RuntimeException {

    public RoomAccessException(String message) {
        super(message);
    }
}
