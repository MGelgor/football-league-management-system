package com.footballleague.entity;

/**
 * Oyun stili: attack takımın kendi gol beklentisini, concede rakibin gol beklentisini çarpar.
 * Hücum daha çok gol attırır ama daha çok yedirir; savunma iki tarafın da golünü azaltır.
 */
public enum PlayStyle {
    ATTACKING(1.15, 1.10),
    BALANCED(1.0, 1.0),
    DEFENSIVE(0.85, 0.85);

    private final double attack;
    private final double concede;

    PlayStyle(double attack, double concede) {
        this.attack = attack;
        this.concede = concede;
    }

    public double attack() {
        return attack;
    }

    public double concede() {
        return concede;
    }
}
