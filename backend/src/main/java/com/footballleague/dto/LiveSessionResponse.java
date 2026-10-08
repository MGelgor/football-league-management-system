package com.footballleague.dto;

import java.util.List;

import com.footballleague.entity.Formation;
import com.footballleague.entity.PlayStyle;
import com.footballleague.entity.Position;

/**
 * Kullanıcının canlı maçı: devre arasında (minute 45) ya da maç sonunda (minute 90, finished). events: maçın o ana
 * kadarki olayları (yorumlu). onPitch / bench yalnızca kullanıcının takımı için.
 */
public record LiveSessionResponse(
        Long matchId,
        int minute,
        boolean finished,
        LiveTimeline.LiveMatch match,
        boolean userHome,
        int homeScore,
        int awayScore,
        List<LiveTimeline.LiveItem> events,
        List<SessionPlayer> onPitch,
        List<SessionPlayer> bench,
        int substitutionsLeft,
        Formation formation,
        PlayStyle playStyle,
        String talkResult,
        List<MatchResponse> otherResults
) {

    public record SessionPlayer(Long id, String name, Position position, int shirtNumber, int strength, double form,
            Double rating) {
    }
}
