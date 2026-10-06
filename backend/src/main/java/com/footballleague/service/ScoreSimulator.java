package com.footballleague.service;

import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

/**
 * Takım gücü + moraline dayalı, rastgele ama güçlü takımı kayıran skor üretir.
 * Beklenen gol sayısı (lambda), takımların göreli gücünden hesaplanır;
 * gerçek skor bu lambda ile Poisson dağılımından örneklenir.
 * Aynı lambdalardan maç öncesi kazanma / beraberlik / kaybetme olasılıkları da hesaplanır.
 */
@Component
public class ScoreSimulator {

    private static final double BASE_GOALS = 1.3;
    private static final int HOME_ADVANTAGE = 5;
    private static final double MORALE_WEIGHT = 0.1;
    private static final int NEUTRAL_MORALE = 50;
    private static final double MIN_EFFECTIVE_STRENGTH = 1.0;
    // Olasılık hesabında bir takımın atabileceği en fazla gol (üstü ihmal edilecek kadar küçük)
    private static final int MAX_GOALS_FOR_PROBABILITY = 12;

    public ExpectedGoals expectedGoals(int homeStrength, int homeMorale, int awayStrength, int awayMorale) {
        double homeEffective = effectiveStrength(homeStrength, homeMorale, HOME_ADVANTAGE);
        double awayEffective = effectiveStrength(awayStrength, awayMorale, 0);

        double strengthRatio = homeEffective / (homeEffective + awayEffective);
        return new ExpectedGoals(BASE_GOALS * 2 * strengthRatio, BASE_GOALS * 2 * (1 - strengthRatio));
    }

    public SimulatedScore simulate(int homeStrength, int homeMorale, int awayStrength, int awayMorale) {
        return simulate(expectedGoals(homeStrength, homeMorale, awayStrength, awayMorale));
    }

    public SimulatedScore simulate(ExpectedGoals expected) {
        return new SimulatedScore(poissonRandom(expected.home()), poissonRandom(expected.away()));
    }

    public Probabilities probabilities(int homeStrength, int homeMorale, int awayStrength, int awayMorale) {
        return probabilities(expectedGoals(homeStrength, homeMorale, awayStrength, awayMorale));
    }

    /** İki bağımsız Poisson dağılımının ortak olasılık tablosu üzerinden: P(ev > dep), P(ev = dep), P(ev < dep). */
    public Probabilities probabilities(ExpectedGoals expected) {
        double homeWin = 0;
        double draw = 0;
        double awayWin = 0;
        for (int home = 0; home <= MAX_GOALS_FOR_PROBABILITY; home++) {
            for (int away = 0; away <= MAX_GOALS_FOR_PROBABILITY; away++) {
                double p = poissonProbability(expected.home(), home) * poissonProbability(expected.away(), away);
                if (home > away) {
                    homeWin += p;
                } else if (home == away) {
                    draw += p;
                } else {
                    awayWin += p;
                }
            }
        }
        double total = homeWin + draw + awayWin;
        return new Probabilities(homeWin / total, draw / total, awayWin / total);
    }

    private double effectiveStrength(int strength, int morale, int advantage) {
        double moraleAdjustment = (morale - NEUTRAL_MORALE) * MORALE_WEIGHT;
        return Math.max(MIN_EFFECTIVE_STRENGTH, strength + advantage + moraleAdjustment);
    }

    static int poissonRandom(double lambda) {
        double limit = Math.exp(-lambda);
        double product = 1.0;
        int count = 0;
        do {
            count++;
            product *= ThreadLocalRandom.current().nextDouble();
        } while (product > limit);
        return count - 1;
    }

    private static double poissonProbability(double lambda, int k) {
        double result = Math.exp(-lambda);
        for (int i = 1; i <= k; i++) {
            result *= lambda / i;
        }
        return result;
    }

    public record ExpectedGoals(double home, double away) {
    }

    public record SimulatedScore(int homeGoals, int awayGoals) {
    }

    public record Probabilities(double homeWin, double draw, double awayWin) {

        public int homeWinPercent() {
            return (int) Math.round(homeWin * 100);
        }

        public int awayWinPercent() {
            return (int) Math.round(awayWin * 100);
        }

        /** Yuvarlama sonrası toplam her zaman 100 olsun diye beraberlik kalan olarak hesaplanır. */
        public int drawPercent() {
            return 100 - homeWinPercent() - awayWinPercent();
        }

        public double homeExpectedPoints() {
            return 3 * homeWin + draw;
        }

        public double awayExpectedPoints() {
            return 3 * awayWin + draw;
        }
    }
}
