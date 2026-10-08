package com.footballleague.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        // Moral farkının etkisi küçük olduğu için rastgele simülasyon yerine kesin olasılıklar karşılaştırılır
        double highMoraleWin = scoreSimulator.probabilities(50, 100, 50, NEUTRAL_MORALE).homeWin();
        double neutralWin = scoreSimulator.probabilities(50, NEUTRAL_MORALE, 50, NEUTRAL_MORALE).homeWin();
        double lowMoraleWin = scoreSimulator.probabilities(50, 0, 50, NEUTRAL_MORALE).homeWin();

        assertTrue(highMoraleWin > neutralWin && neutralWin > lowMoraleWin,
                "Yuksek moral kazanma olasiligini arttirmali: " + highMoraleWin + " > " + neutralWin + " > " + lowMoraleWin);
    }

    @Test
    void olasiliklarinToplami100VeYuzdelerTutarli() {
        ScoreSimulator.Probabilities probabilities = scoreSimulator.probabilities(70, 60, 45, 40);

        assertEquals(100, probabilities.homeWinPercent() + probabilities.drawPercent() + probabilities.awayWinPercent());
        assertEquals(1.0, probabilities.homeWin() + probabilities.draw() + probabilities.awayWin(), 1e-9);
    }

    @Test
    void gucluTakiminKazanmaOlasiligiDahaYuksekVeEsitGucteEvSahibiHafifOnde() {
        ScoreSimulator.Probabilities strongHome = scoreSimulator.probabilities(90, NEUTRAL_MORALE, 20, NEUTRAL_MORALE);
        ScoreSimulator.Probabilities even = scoreSimulator.probabilities(60, NEUTRAL_MORALE, 60, NEUTRAL_MORALE);

        assertTrue(strongHome.homeWinPercent() > 60, "Guclu ev sahibi %60'tan fazla: " + strongHome.homeWinPercent());
        assertTrue(strongHome.awayWinPercent() < 15, "Zayif deplasman %15'ten az: " + strongHome.awayWinPercent());
        assertTrue(even.homeWin() > even.awayWin(), "Esit gucte ev sahibi avantaji olmali");
    }

    @Test
    void hesaplananOlasilikSimulasyonSonuclariylaUyumlu() {
        ScoreSimulator.Probabilities probabilities = scoreSimulator.probabilities(80, NEUTRAL_MORALE, 40, NEUTRAL_MORALE);
        int homeWins = 0;
        int runs = 20_000;
        for (int i = 0; i < runs; i++) {
            ScoreSimulator.SimulatedScore score = scoreSimulator.simulate(80, NEUTRAL_MORALE, 40, NEUTRAL_MORALE);
            if (score.homeGoals() > score.awayGoals()) {
                homeWins++;
            }
        }
        assertEquals(probabilities.homeWin(), (double) homeWins / runs, 0.02,
                "Gosterilen olasilik ile simulasyondaki kazanma orani 2 puandan fazla farkli olmamali");
    }
}
