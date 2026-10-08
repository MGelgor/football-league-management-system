package com.footballleague.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.footballleague.entity.Formation;
import com.footballleague.entity.MatchLineup;
import com.footballleague.entity.PlayStyle;
import com.footballleague.entity.Player;
import com.footballleague.entity.Team;

/**
 * Maçtaki taktik kurulumu: takımın dizilişi, teknik direktör bonusu ve stil. Yapay zekâ, rakip belirgin şekilde
 * zayıfsa hücuma, belirgin şekilde güçlüyse savunmaya geçer; arada takımın varsayılan stilini oynar.
 */
@Component
public class TacticsService {

    // Ev sahibi avantajı dahil güç farkı bu kadarı geçerse stil değişir
    static final int STYLE_SWITCH_GAP = 12;
    private static final int HOME_EDGE = 5;
    static final double MIN_LINEUP_QUALITY = 0.8;

    public ScoreSimulator.TeamSetup setup(Team team, Team opponent, boolean home) {
        return setup(team, opponent, home, null);
    }

    /** lineup: kullanıcının seçtiği kadro varsa diziliş ve stil ondan gelir (yapay zekâ stili değiştirmez). */
    public ScoreSimulator.TeamSetup setup(Team team, Team opponent, boolean home, MatchLineup lineup) {
        Formation formation = lineup != null ? lineup.getFormation() : team.getFormation();
        PlayStyle style = lineup != null ? lineup.getPlayStyle() : chooseStyle(team, opponent, home);
        return new ScoreSimulator.TeamSetup(team.matchStrength(), team.getMorale(), formation, style,
                team.getManager() != null ? team.getManager().strengthBonus() : 0);
    }

    /**
     * Seçilen ilk 11'in kalitesi: efektif güç ortalamasının, aynı dizilişte en iyi 11'in ortalamasına oranı
     * (0.8-1.0). Yapay zekâ en iyi 11'e yakın seçtiği için ~1; kullanıcı zayıf oyuncularla çıkarsa gol beklentisi düşer.
     */
    static double lineupQuality(MatchDetailGenerator.SideSetup side) {
        if (side.starters() == null || side.available().isEmpty()) {
            return 1.0;
        }
        double chosen = averageEffective(side.starters());
        double best = averageEffective(MatchDetailGenerator.pickStartingEleven(side.available(), side.formation(), 0));
        return best == 0 ? 1.0 : Math.clamp(chosen / best, MIN_LINEUP_QUALITY, 1.0);
    }

    private static double averageEffective(List<Player> players) {
        return players.stream().mapToDouble(Player::effectiveStrength).average().orElse(0);
    }

    static PlayStyle chooseStyle(Team team, Team opponent, boolean home) {
        int gap = team.matchStrength() - opponent.matchStrength() + (home ? HOME_EDGE : -HOME_EDGE);
        if (gap >= STYLE_SWITCH_GAP) {
            return PlayStyle.ATTACKING;
        }
        if (gap <= -STYLE_SWITCH_GAP) {
            return PlayStyle.DEFENSIVE;
        }
        return team.getPlayStyle();
    }
}
