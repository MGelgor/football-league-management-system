package com.footballleague.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ScoreSimulatorTest {

    private static final int SIMULATIONS = 1000;
    private static final int NEUTRAL_MORALE = 50;

    private final ScoreSimulator scoreSimulator = new ScoreSimulator();

    @Test
    void gucluTakimZayifTakimdanBelirginSekildeDahaSikKazanir() {
        int strongStrength = 90;
        int weakStrength = 20;

        int strongWins = 0;
        int weakWins = 0;

        for (int i = 0; i < SIMULATIONS; i++) {
            ScoreSimulator.SimulatedScore score = scoreSimulator.simulate(
                    strongStrength, NEUTRAL_MORALE, weakStrength, NEUTRAL_MORALE);
            if (score.homeGoals() > score.awayGoals()) {
                strongWins++;
            } else if (score.homeGoals() < score.awayGoals()) {
                weakWins++;
            }
        }

        assertTrue(strongWins > SIMULATIONS / 2,
                "Guclu takim maclarin yarisindan fazlasini kazanmali. Guclu: " + strongWins);
        assertTrue(strongWins > weakWins * 3,
                "Guclu takim, zayif takimdan en az 3 kat fazla kazanmali. Guclu: " + strongWins + ", zayif: " + weakWins);
    }

    @Test
    void esitGucteTakimlarAsiriTekTarafliSonucUretmez() {
        int homeWins = 0;
        int awayWins = 0;

        for (int i = 0; i < SIMULATIONS; i++) {
            ScoreSimulator.SimulatedScore score = scoreSimulator.simulate(60, NEUTRAL_MORALE, 60, NEUTRAL_MORALE);
            if (score.homeGoals() > score.awayGoals()) {
                homeWins++;
            } else if (score.homeGoals() < score.awayGoals()) {
                awayWins++;
            }
        }

        assertTrue(homeWins < SIMULATIONS * 0.65, "Ev sahibi avantaji asiri baskin olmamali. Ev sahibi kazanma: " + homeWins);
        assertTrue(awayWins > SIMULATIONS * 0.15, "Deplasman takimi da makul oranda kazanabilmeli. Deplasman kazanma: " + awayWins);
    }

    @Test
    void ureteilenSkorlarNegatifOlamaz() {
        for (int i = 0; i < SIMULATIONS; i++) {
            ScoreSimulator.SimulatedScore score = scoreSimulator.simulate(50, NEUTRAL_MORALE, 50, NEUTRAL_MORALE);
            assertTrue(score.homeGoals() >= 0, "Gol sayisi negatif olamaz");
            assertTrue(score.awayGoals() >= 0, "Gol sayisi negatif olamaz");
        }
    }

    @Test
    void yuksekMoralKazanmaSansiniArttirir() {
        int highMoraleWins = 0;
        int lowMoraleWins = 0;

        for (int i = 0; i < SIMULATIONS; i++) {
            // Ayni guce sahip takimlar, ama ev sahibinin morali yuksek/dusuk
            ScoreSimulator.SimulatedScore highMoraleGame = scoreSimulator.simulate(50, 100, 50, NEUTRAL_MORALE);
            if (highMoraleGame.homeGoals() > highMoraleGame.awayGoals()) {
                highMoraleWins++;
            }

            ScoreSimulator.SimulatedScore lowMoraleGame = scoreSimulator.simulate(50, 0, 50, NEUTRAL_MORALE);
            if (lowMoraleGame.homeGoals() > lowMoraleGame.awayGoals()) {
                lowMoraleWins++;
            }
        }

        assertTrue(highMoraleWins > lowMoraleWins,
                "Yuksek moralli takim, dusuk moralli takimdan daha sik kazanmali. Yuksek: " + highMoraleWins
                        + ", dusuk: " + lowMoraleWins);
    }
}
