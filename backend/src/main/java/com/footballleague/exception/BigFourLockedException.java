package com.footballleague.exception;

public class BigFourLockedException extends RuntimeException {

    public BigFourLockedException(String teamName) {
        super(teamName + " 4 büyüklerden biri: sistem tarafından tanımlıdır, düzenlenemez ve silinemez");
    }
}
