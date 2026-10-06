package com.footballleague.entity;

/** Kupa turları; bir turdaki maç sayısı = takım sayısı / 2. */
public enum CupRound {
    QUARTER_FINAL(4),
    SEMI_FINAL(2),
    FINAL(1);

    private final int matchCount;

    CupRound(int matchCount) {
        this.matchCount = matchCount;
    }

    public int matchCount() {
        return matchCount;
    }

    public CupRound next() {
        return this == FINAL ? null : values()[ordinal() + 1];
    }
}
