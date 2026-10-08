package com.footballleague.entity;

/**
 * LEAGUE: 1. Lig (haftalar 1..N), SECOND_LEAGUE: 2. Lig (haftalar 201..200+N, 1. Lig haftasıyla birlikte oynanır),
 * CUP: kupa (101+).
 */
public enum Competition {
    LEAGUE,
    CUP,
    SECOND_LEAGUE;

    // 2. Lig haftaları 1. Lig ve kupa haftalarıyla karışmasın diye 200'den başlar
    public static final int SECOND_LEAGUE_WEEK_OFFSET = 200;

    /** Lig maçı mı (1. ya da 2. Lig): moral, güç ve maaşlar yalnızca lig maçlarında işler. */
    public boolean isLeague() {
        return this == LEAGUE || this == SECOND_LEAGUE;
    }

    public static Competition ofDivision(int division) {
        return division == 2 ? SECOND_LEAGUE : LEAGUE;
    }

    /** Ekranda gösterilen hafta numarası (2. Lig'de 201 → 1). */
    public int displayWeek(int weekNumber) {
        return this == SECOND_LEAGUE ? weekNumber - SECOND_LEAGUE_WEEK_OFFSET : weekNumber;
    }
}
