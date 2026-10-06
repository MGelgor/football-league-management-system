package com.footballleague.exception;

public class SeasonNotFoundException extends RuntimeException {

    public SeasonNotFoundException(Long id) {
        super("Sezon bulunamadı: " + id);
    }
}
