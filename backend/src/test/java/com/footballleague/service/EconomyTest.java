package com.footballleague.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.footballleague.entity.Position;
import com.footballleague.entity.Team;

class EconomyTest {

    @Test
    void piyasaDegeriGucleArtarGencVeForvetDahaDegerli() {
        assertEquals(1_000_000, Economy.marketValue(60, 27, Position.DEFENDER) * 10 / 9, 20_000);
        assertTrue(Economy.marketValue(80, 27, Position.MIDFIELDER) > Economy.marketValue(70, 27, Position.MIDFIELDER));
        assertTrue(Economy.marketValue(70, 20, Position.MIDFIELDER) > Economy.marketValue(70, 27, Position.MIDFIELDER));
        assertTrue(Economy.marketValue(70, 27, Position.MIDFIELDER) > Economy.marketValue(70, 34, Position.MIDFIELDER));
        assertTrue(Economy.marketValue(70, 27, Position.FORWARD) > Economy.marketValue(70, 27, Position.GOALKEEPER));
        assertEquals(0, Economy.marketValue(73, 24, Position.FORWARD) % 10_000, "10 000'e yuvarlı");
    }

    @Test
    void maasDegerinBindeDordu() {
        assertEquals(4_000, Economy.weeklyWage(1_000_000));
        assertEquals(500, Economy.weeklyWage(10_000), "En az 500");
    }

    @Test
    void ligOdulu20MilyondanIkiMilyonaDogrusalBiletGeliriGucleArtar() {
        assertEquals(20_000_000, Economy.leaguePrize(1, 18));
        assertEquals(2_000_000, Economy.leaguePrize(18, 18));
        assertTrue(Economy.leaguePrize(5, 18) > Economy.leaguePrize(6, 18));

        Team weak = Team.builder().strength(40).build();
        Team strong = Team.builder().strength(80).build();
        Team big = Team.builder().strength(80).bigFour(true).build();
        assertTrue(Economy.ticketIncome(strong) > Economy.ticketIncome(weak) * 10);
        assertTrue(Economy.ticketIncome(big) > Economy.ticketIncome(strong));
        assertEquals(Economy.ticketIncome(strong) * 20, Economy.initialBudget(strong));
    }
}
