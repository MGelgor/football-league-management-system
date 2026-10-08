package com.footballleague.exception;

/** Menajer modu akışında yapılamayan işlem (mod kapalı, sırada maç yok, canlı maç oturumu yok vb.). */
public class ManagerModeException extends RuntimeException {

    public ManagerModeException(String message) {
        super(message);
    }
}
