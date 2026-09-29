package com.footballleague.exception;

public class TeamsLockedException extends RuntimeException {

    public TeamsLockedException() {
        super("Fikstür oluşturulduktan sonra takım eklenemez veya silinemez. Önce fikstürü sıfırlayın.");
    }
}
