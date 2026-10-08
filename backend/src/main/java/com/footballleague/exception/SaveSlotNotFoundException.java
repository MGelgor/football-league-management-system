package com.footballleague.exception;

public class SaveSlotNotFoundException extends RuntimeException {

    public SaveSlotNotFoundException(String name) {
        super("Kayıt bulunamadı: " + name);
    }
}
