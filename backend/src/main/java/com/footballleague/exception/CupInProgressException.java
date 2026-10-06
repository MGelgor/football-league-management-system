package com.footballleague.exception;

public class CupInProgressException extends RuntimeException {

    public CupInProgressException() {
        super("Kupa devam ediyor. Yeni sezona geçmeden önce kupayı tamamlayın.");
    }
}
