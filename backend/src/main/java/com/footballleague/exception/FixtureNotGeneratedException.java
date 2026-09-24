package com.footballleague.exception;

public class FixtureNotGeneratedException extends RuntimeException {

    public FixtureNotGeneratedException() {
        super("Fikstür henüz oluşturulmamış. Önce POST /api/fixtures/generate çağırın.");
    }
}
