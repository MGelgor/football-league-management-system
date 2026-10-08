package com.footballleague.exception;

public class WeekNotPlayedException extends RuntimeException {

    public WeekNotPlayedException(int weekNumber) {
        super((weekNumber > 100 ? "Bu kupa turu" : "Hafta " + weekNumber) + " henüz oynanmadı");
    }
}
