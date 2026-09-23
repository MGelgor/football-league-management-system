package com.footballleague.exception;

public class DuplicateTeamNameException extends RuntimeException {

    public DuplicateTeamNameException(String name) {
        super("'" + name + "' adında bir takım zaten mevcut");
    }
}
