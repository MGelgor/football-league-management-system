package com.footballleague.dto;

import java.util.List;

import com.footballleague.entity.Formation;
import com.footballleague.entity.PlayStyle;
import com.footballleague.entity.Position;

/**
 * Sıradaki rakibin analizi (kadro hazırlığı için): güç / moral / hoca, puan durumu ve form, beklenen stil, dizilişine
 * karşı avantajlı dizilişler, muhtemel ilk 11, öne çıkan oyuncular, eksikler ve son maçlar.
 * strengthGap: rakibin maç gücü − senin maç gücün (4 büyükler bonusu dahil).
 */
public record OpponentReportResponse(
        TeamResponse team,
        boolean opponentHome,
        int strengthGap,
        StandingResponse standing,
        Formation formation,
        PlayStyle expectedStyle,
        List<Formation> strongAgainst,
        List<Formation> weakAgainst,
        List<ScoutPlayer> probableEleven,
        List<ScoutPlayer> keyPlayers,
        List<ScoutPlayer> unavailable,
        List<MyTeamResponse.RecentResult> recentResults
) {

    /** note: eksik oyuncuda sakatlık / ceza açıklaması. */
    public record ScoutPlayer(Long id, String name, Position position, int shirtNumber, Integer age, int strength,
            double effectiveStrength, double form, int fatigue, String note) {
    }
}
