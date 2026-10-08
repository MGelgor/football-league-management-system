package com.footballleague.entity;

/** Sakatlık türü: hafif 1-2, orta 3-6, uzun 8-20 maç. */
public enum InjurySeverity {
    MINOR,
    MODERATE,
    SERIOUS;

    public static InjurySeverity of(int matches) {
        if (matches <= 2) {
            return MINOR;
        }
        return matches <= 6 ? MODERATE : SERIOUS;
    }
}
