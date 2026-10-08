package com.footballleague.dto;

/** status: ACCEPTED (transfer yapıldı), COUNTER (askingPrice karşı teklif), REJECTED. */
public record OfferResponse(String status, long askingPrice, String message, TransferResponse transfer) {
}
