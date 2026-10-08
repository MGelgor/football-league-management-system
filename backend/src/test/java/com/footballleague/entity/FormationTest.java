package com.footballleague.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class FormationTest {

    @Test
    void herDizilisIkiDizilisiYenerIkisineYenilirAvantajSimetrik() {
        for (Formation formation : Formation.values()) {
            long wins = Arrays.stream(Formation.values()).filter(other -> formation.advantageOver(other) > 0).count();
            long losses = Arrays.stream(Formation.values()).filter(other -> formation.advantageOver(other) < 0).count();
            assertEquals(2, wins, formation.label());
            assertEquals(2, losses, formation.label());
            assertEquals(0, formation.advantageOver(formation));
            for (Formation other : Formation.values()) {
                assertEquals(-formation.advantageOver(other), other.advantageOver(formation));
            }
        }
    }

    @Test
    void herDizilisteOnBirOyuncuVeEtiketMevkiSayilariylaUyumlu() {
        for (Formation formation : Formation.values()) {
            int total = Arrays.stream(Position.values()).mapToInt(formation::count).sum();
            assertEquals(11, total);
            assertEquals(formation.count(Position.DEFENDER) + "-" + formation.count(Position.MIDFIELDER) + "-"
                    + formation.count(Position.FORWARD), formation.label());
        }
    }
}
