package com.footballleague.exception;

public class TeamsLockedException extends RuntimeException {

    public TeamsLockedException() {
        super("Sezon devam ederken takım eklenemez veya silinemez. Sezonu tamamlayın ya da sıfırlayın.");
    }
}
