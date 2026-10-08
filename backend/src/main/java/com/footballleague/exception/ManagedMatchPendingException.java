package com.footballleague.exception;

public class ManagedMatchPendingException extends RuntimeException {

    public ManagedMatchPendingException(String teamName) {
        super(teamName + " bu turda oynuyor: önce \"Takımım\" sayfasından kadroyu belirleyin ya da maçı oradan oynatın");
    }
}
