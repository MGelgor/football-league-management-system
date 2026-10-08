package com.footballleague.dto;

/** upcomingSeasonNumber: pencere açıkken transferlerin geçerli olacağı sezon. */
public record TransferWindowResponse(boolean open, Integer upcomingSeasonNumber, String message) {
}
