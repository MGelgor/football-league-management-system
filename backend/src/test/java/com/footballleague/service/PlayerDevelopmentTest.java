package com.footballleague.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class PlayerDevelopmentTest {

    private static final int RUNS = 1000;

    private final PlayerDevelopment development = new PlayerDevelopment();

    @Test
    void gencOyuncuGelisirYasliOyuncuGeriler() {
        assertTrue(IntStream.range(0, RUNS).allMatch(i -> development.strengthChange(19, null) > 0),
                "21 yas alti her zaman gelisir");
        assertTrue(IntStream.range(0, RUNS).allMatch(i -> development.strengthChange(34, null) < 0),
                "33 yas ustu her zaman geriler");
    }

    @Test
    void iyiSezonGelisimiArttirirKotuSezonAzaltir() {
        double good = IntStream.range(0, RUNS).map(i -> development.strengthChange(27, 7.5)).average().orElseThrow();
        double neutral = IntStream.range(0, RUNS).map(i -> development.strengthChange(27, 6.5)).average().orElseThrow();
        double bad = IntStream.range(0, RUNS).map(i -> development.strengthChange(27, 5.5)).average().orElseThrow();

        assertTrue(good > neutral + 0.8 && neutral > bad + 0.8, good + " / " + neutral + " / " + bad);
    }

    @Test
    void emeklilik35YastanOnceOlmaz38deKesin() {
        assertFalse(IntStream.range(0, RUNS).anyMatch(i -> development.retires(34)));
        assertTrue(IntStream.range(0, RUNS).allMatch(i -> development.retires(38)));
        long at36 = IntStream.range(0, RUNS).filter(i -> development.retires(36)).count();
        assertTrue(at36 > RUNS * 0.4 && at36 < RUNS * 0.6, "36 yasinda ~%50: " + at36);
    }
}
