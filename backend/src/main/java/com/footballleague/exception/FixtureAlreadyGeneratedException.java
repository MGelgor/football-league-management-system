package com.footballleague.exception;

public class FixtureAlreadyGeneratedException extends RuntimeException {

    public FixtureAlreadyGeneratedException() {
        super("Fikstür zaten oluşturulmuş. Yeniden oluşturmak için önce mevcut fikstürü silin.");
    }
}
