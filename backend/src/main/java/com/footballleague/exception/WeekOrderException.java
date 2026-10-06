package com.footballleague.exception;

public class WeekOrderException extends RuntimeException {

    public WeekOrderException(Integer requestedWeek, Integer nextWeek) {
        super("Haftalar sırayla oynanmalı: Hafta " + requestedWeek + " oynatılamaz, sıradaki hafta " + nextWeek);
    }
}
