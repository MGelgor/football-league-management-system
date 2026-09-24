package com.footballleague.exception;

public class MatchWeekNotFoundException extends RuntimeException {

    public MatchWeekNotFoundException(Integer weekNumber) {
        super("Hafta bulunamadı: hafta=" + weekNumber);
    }
}
