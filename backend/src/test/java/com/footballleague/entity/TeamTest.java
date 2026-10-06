package com.footballleague.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TeamTest {

    @Test
    void normalTakimGucu1Ile84ArasindaKalir() {
        Team team = Team.builder().strength(83).build();

        team.changeStrength(5);
        assertEquals(Team.REGULAR_MAX_STRENGTH, team.getStrength());
        assertEquals(1, team.getLastStrengthChange(), "Son degisim, sinirlandirilmis gercek degisim olmali");

        team.setStrength(2);
        team.changeStrength(-5);
        assertEquals(Team.MIN_STRENGTH, team.getStrength());
        assertEquals(-1, team.getLastStrengthChange());
    }

    @Test
    void dortBuyuklerinGucu85Ile100ArasindaKalir() {
        Team team = Team.builder().strength(86).bigFour(true).build();

        team.changeStrength(-4);
        assertEquals(Team.BIG_FOUR_MIN_STRENGTH, team.getStrength());

        team.setStrength(99);
        team.changeStrength(4);
        assertEquals(Team.MAX_STRENGTH, team.getStrength());
    }

    @Test
    void sezonlukGucDegisimiSezonBasindakiDegereGoreHesaplanir() {
        Team team = Team.builder().strength(60).seasonStartStrength(60).build();

        team.changeStrength(2);
        team.changeStrength(-1);
        team.changeStrength(2);

        assertEquals(3, team.seasonStrengthChange());
        assertEquals(2, team.getLastStrengthChange());
    }

    @Test
    void dortBuyuklereMacHesabindaBonusEklenirGosterilenGucDegismez() {
        Team big = Team.builder().strength(90).bigFour(true).build();
        Team regular = Team.builder().strength(70).build();

        assertEquals(90 + Team.BIG_FOUR_MATCH_BONUS, big.matchStrength());
        assertEquals(90, big.getStrength());
        assertEquals(70, regular.matchStrength());
    }
}
