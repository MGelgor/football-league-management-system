package com.footballleague.dto;

import java.util.List;

import com.footballleague.entity.Formation;
import com.footballleague.entity.InjurySeverity;
import com.footballleague.entity.PlayStyle;
import com.footballleague.entity.Position;

/** Sıradaki maç için kayıtlı kadro; saved false ise yapay zekânın önerisi. squad: tüm kadro ve uygunluk. */
public record LineupResponse(
        Long matchId,
        boolean saved,
        Formation formation,
        PlayStyle playStyle,
        List<Long> starterIds,
        Long captainId,
        Long penaltyTakerId,
        List<SquadPlayer> squad
) {

    public record SquadPlayer(Long id, String name, Position position, int shirtNumber, int strength,
            double effectiveStrength, double form, int fatigue, boolean available, int suspendedMatches,
            int injuredMatches, InjurySeverity injurySeverity) {
    }
}
