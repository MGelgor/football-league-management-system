package com.footballleague.entity;

/** Kapalı teklifin durumu: pencere kapanana kadar bekler, sonra kazanır, kaybeder ya da geçersiz kalır. */
public enum BidStatus {
    PENDING,
    WON,
    LOST,
    FAILED
}
