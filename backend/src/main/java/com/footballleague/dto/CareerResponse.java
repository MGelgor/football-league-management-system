package com.footballleague.dto;

import java.util.List;

/** Menajerin kariyeri: takım takım dönemleri ve toplamlar. */
public record CareerResponse(
        String managerName,
        List<Spell> spells,
        int matches,
        int wins,
        int draws,
        int losses,
        int leagueTitles,
        int cups
) {

    public record Spell(Long teamId, String teamName, int startSeason, Integer endSeason, String endReason,
            int matches, int wins, int draws, int losses, int leagueTitles, int cups) {
    }
}
