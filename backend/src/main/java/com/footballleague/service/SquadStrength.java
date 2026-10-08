package com.footballleague.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.footballleague.entity.Formation;
import com.footballleague.entity.Player;
import com.footballleague.entity.Position;

/** Kadronun gücü: dizilişe göre her mevkideki en güçlü oyuncuların (ilk 11) ortalaması. */
final class SquadStrength {

    private SquadStrength() {
    }

    static double topElevenAverage(List<Player> squad, Formation formation) {
        List<Integer> starters = new ArrayList<>();
        for (Position position : Position.values()) {
            squad.stream()
                    .filter(player -> player.getPosition() == position)
                    .map(Player::getStrength)
                    .sorted(Comparator.reverseOrder())
                    .limit(formation.count(position))
                    .forEach(starters::add);
        }
        return starters.stream().mapToInt(Integer::intValue).average().orElse(0);
    }

    /** Mevkideki en zayıf ilk 11 oyuncusu (o mevkide hiç oyuncu yoksa null). */
    static Player weakestStarter(List<Player> squad, Formation formation, Position position) {
        return squad.stream()
                .filter(player -> player.getPosition() == position)
                .sorted(Comparator.comparingInt(Player::getStrength).reversed())
                .limit(formation.count(position))
                .min(Comparator.comparingInt(Player::getStrength))
                .orElse(null);
    }
}
