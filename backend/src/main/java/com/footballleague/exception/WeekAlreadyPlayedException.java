package com.footballleague.exception;

public class WeekAlreadyPlayedException extends RuntimeException {

    public WeekAlreadyPlayedException(Integer weekNumber) {
        super("Hafta " + weekNumber + " zaten oynanmış");
    }
}
