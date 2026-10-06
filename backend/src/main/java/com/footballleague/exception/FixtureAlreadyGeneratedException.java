package com.footballleague.exception;

public class FixtureAlreadyGeneratedException extends RuntimeException {

    public FixtureAlreadyGeneratedException() {
        super("Devam eden bir sezon var. Yeni fikstür için önce sezonu tamamlayın ya da sıfırlayın.");
    }
}
