package com.footballleague.exception;

public class MatchNotFoundException extends RuntimeException {

    public MatchNotFoundException(Long id) {
        super("Maç bulunamadı: " + id);
    }
}
