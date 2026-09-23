package com.footballleague.exception;

public class TeamNotFoundException extends RuntimeException {

    public TeamNotFoundException(Long id) {
        super("Takım bulunamadı: id=" + id);
    }
}
