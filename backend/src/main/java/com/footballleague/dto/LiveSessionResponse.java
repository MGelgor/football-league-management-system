package com.footballleague.dto;

import java.util.List;

import com.footballleague.entity.Formation;
import com.footballleague.entity.PlayStyle;
import com.footballleague.entity.Position;

/**
 * Kullanıcının canlı maçı: devre arasında (minute 45) ya da maç sonunda (minute 90, finished). events: maçın o ana
 * kadarki olayları (yorumlu). onPitch / bench yalnızca kullanıcının takımı için; pitch iki takımın o ana kadar sahaya
 * çıkan tüm oyuncuları (yatay saha görünümü). resumed: devre arasında bekleyen oturuma geri dönüldü.
 * moraleChange / morale: maç sonunda kullanıcının takımının moral değişimi (konuşma dahil) ve yeni morali.
 */
public record LiveSessionResponse(
        Long matchId,
        int minute,
        boolean finished,
        boolean resumed,
        LiveTimeline.LiveMatch match,
        boolean userHome,
        int homeScore,
        int awayScore,
        List<LiveTimeline.LiveItem> events,
        List<SessionPlayer> onPitch,
        List<SessionPlayer> bench,
        List<PitchPlayer> pitch,
        int substitutionsLeft,
        Formation formation,
        Formation opponentFormation,
        PlayStyle playStyle,
        String talkResult,
        Integer moraleChange,
        Integer morale,
        List<MatchResponse> otherResults
) {

    /**
     * strength: güç; currentStrength: o anki güç (form, üst üste maç ve maç içi yorgunluk dahil); matchFatigue: maç
     * içinde düşen güç; rating yalnızca maç sonunda.
     */
    public record SessionPlayer(Long id, String name, Position position, int shirtNumber, int strength, double form,
            double currentStrength, double matchFatigue, Double rating) {
    }

    /**
     * Sahadaki bir oyuncu: minuteOn (ilk 11 = 0), minuteOff (hâlâ sahadaysa null), replacedPlayerId (yerine girdiği),
     * fatigueRate (90 dakikada düşen güç; istemci dakikaya göre yorgunluğu hesaplar).
     */
    public record PitchPlayer(Long id, String name, Position position, int shirtNumber, boolean home,
            double effectiveStrength, double fatigueRate, int minuteOn, Integer minuteOff, Long replacedPlayerId) {
    }
}
