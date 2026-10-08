package com.footballleague.dto;

/** Menajer işlemlerinin sonucu: status ACCEPTED / COUNTER / REJECTED / DONE, amount (ör. oyuncunun maaş isteği). */
public record ActionResponse(String status, Long amount, String message) {
}
