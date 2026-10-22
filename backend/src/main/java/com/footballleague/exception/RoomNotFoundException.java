package com.footballleague.exception;

public class RoomNotFoundException extends RuntimeException {

    public RoomNotFoundException(String code) {
        super("Oda bulunamadı: " + code);
    }
}
