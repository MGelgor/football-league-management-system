package com.footballleague.exception;

public class SeasonFinishedException extends RuntimeException {

    public SeasonFinishedException() {
        super("Sezon tamamlandı. Tamamlanmış sezon sıfırlanamaz ve oynatılamaz; yeni sezonu başlatın.");
    }
}
