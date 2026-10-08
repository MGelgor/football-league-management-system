package com.footballleague.exception;

public class TransferWindowClosedException extends RuntimeException {

    public TransferWindowClosedException() {
        super("Transfer penceresi kapalı: transferler lig sezonu bittikten sonra, yeni sezon başlayana kadar yapılır");
    }
}
