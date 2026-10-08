package com.footballleague.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class PlayerTest {

    @Test
    void formSonBesMacinReytingindenHesaplanirVeSinirlanir() {
        Player player = Player.builder().strength(60).build();
        assertEquals(1.0, player.form(), "Hiç maç yoksa nötr");

        for (double rating : new double[] {5.0, 6.0, 7.0, 8.0, 9.0, 9.0}) {
            player.addRating(rating);
        }
        assertEquals(List.of(6.0, 7.0, 8.0, 9.0, 9.0), player.ratingHistory(), "Yalnızca son 5 maç");
        assertEquals(1.1, player.form(), 1e-9, "Ortalama 7.8 → üst sınır 1.1");

        Player poor = Player.builder().strength(60).build();
        poor.addRating(6.0);
        assertEquals(0.96, poor.form(), 1e-9);
        poor.addRating(3.0);
        assertEquals(0.9, poor.form(), 1e-9, "Alt sınır");
    }

    @Test
    void yorgunlukUcMactanSonraBaslarVeEfektifGucuDusurur() {
        Player player = Player.builder().strength(70).consecutiveStarts(3).build();
        assertEquals(0, player.fatigue());
        player.setConsecutiveStarts(5);
        assertEquals(4, player.fatigue());
        assertEquals(66, player.effectiveStrength(), 1e-9);
        player.setConsecutiveStarts(20);
        assertEquals(8, player.fatigue(), "En fazla 8");
    }

    @Test
    void sakatlikTuruSureyeGoreBelirlenir() {
        assertEquals(InjurySeverity.MINOR, InjurySeverity.of(2));
        assertEquals(InjurySeverity.MODERATE, InjurySeverity.of(3));
        assertEquals(InjurySeverity.MODERATE, InjurySeverity.of(6));
        assertEquals(InjurySeverity.SERIOUS, InjurySeverity.of(8));
    }
}
