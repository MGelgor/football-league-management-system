package com.footballleague.exception;

public class PlayerNotFoundException extends RuntimeException {

    public PlayerNotFoundException(Long id) {
        super("Oyuncu bulunamadı: " + id);
    }
}
