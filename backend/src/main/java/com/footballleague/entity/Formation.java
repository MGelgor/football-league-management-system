package com.footballleague.entity;

import java.util.Map;

/**
 * Diziliş: ilk 11'deki mevki sayıları (1 kaleci + defans-orta saha-forvet). Her diziliş iki dizilişe karşı küçük
 * bir avantaj sağlar, ikisine karşı dezavantajlıdır (taş-kâğıt-makas gibi döngüsel): sıradaki bir sonraki ve üç
 * sonraki dizilişi yener.
 */
public enum Formation {
    F442("4-4-2", 4, 4, 2),
    F433("4-3-3", 4, 3, 3),
    F352("3-5-2", 3, 5, 2),
    F532("5-3-2", 5, 3, 2),
    F451("4-5-1", 4, 5, 1);

    // Maç hesabında efektif güce eklenen / çıkarılan
    public static final int ADVANTAGE = 2;

    private final String label;
    private final Map<Position, Integer> counts;

    Formation(String label, int defenders, int midfielders, int forwards) {
        this.label = label;
        this.counts = Map.of(Position.GOALKEEPER, 1, Position.DEFENDER, defenders, Position.MIDFIELDER, midfielders,
                Position.FORWARD, forwards);
    }

    public String label() {
        return label;
    }

    public int count(Position position) {
        return counts.get(position);
    }

    /** +ADVANTAGE (bu diziliş rakibi yener), -ADVANTAGE ya da 0 (aynı diziliş). */
    public int advantageOver(Formation other) {
        int size = values().length;
        int distance = Math.floorMod(other.ordinal() - ordinal(), size);
        if (distance == 0) {
            return 0;
        }
        return distance == 1 || distance == 3 ? ADVANTAGE : -ADVANTAGE;
    }
}
