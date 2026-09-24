package com.footballleague.service;

import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

/**
 * Takım gücü + moraline dayalı, rastgele ama güçlü takımı kayıran skor üretir.
 * Beklenen gol sayısı (lambda), takımların göreli gücünden hesaplanır;
 * gerçek skor bu lambda ile Poisson dağılımından örneklenir.
 */
@Component
public class ScoreSimulator {

    private static final double BASE_GOALS = 1.3;
    private static final int HOME_ADVANTAGE = 5;
    private static final double MORALE_WEIGHT = 0.1;
    private static final int NEUTRAL_MORALE = 50;
    private static final double MIN_EFFECTIVE_STRENGTH = 1.0;

    public SimulatedScore simulate(int homeStrength, int homeMorale, int awayStrength, int awayMorale) {
        double homeEffective = effectiveStrength(homeStrength, homeMorale, HOME_ADVANTAGE);
        double awayEffective = effectiveStrength(awayStrength, awayMorale, 0);

        double strengthRatio = homeEffective / (homeEffective + awayEffective);
        double homeLambda = BASE_GOALS * 2 * strengthRatio;
        double awayLambda = BASE_GOALS * 2 * (1 - strengthRatio);

        return new SimulatedScore(poissonRandom(homeLambda), poissonRandom(awayLambda));
    }

    private double effectiveStrength(int strength, int morale, int advantage) {
        double moraleAdjustment = (morale - NEUTRAL_MORALE) * MORALE_WEIGHT;
        return Math.max(MIN_EFFECTIVE_STRENGTH, strength + advantage + moraleAdjustment);
    }

    private int poissonRandom(double lambda) {
        double limit = Math.exp(-lambda);
        double product = 1.0;
        int goals = 0;
        do {
            goals++;
            product *= ThreadLocalRandom.current().nextDouble();
        } while (product > limit);
        return goals - 1;
    }

    public record SimulatedScore(int homeGoals, int awayGoals) {
    }
}
