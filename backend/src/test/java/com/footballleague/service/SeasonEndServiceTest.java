package com.footballleague.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;

import com.footballleague.dto.StandingResponse;

class SeasonEndServiceTest {

    @Test
    void sezonSonuGucDegisimiSiralamayaGoreDir() {
        assertEquals(4, SeasonEndService.seasonEndStrengthChange(1, 18));
        assertEquals(2, SeasonEndService.seasonEndStrengthChange(4, 18));
        assertEquals(1, SeasonEndService.seasonEndStrengthChange(9, 18));
        assertEquals(-1, SeasonEndService.seasonEndStrengthChange(10, 18));
        assertEquals(-3, SeasonEndService.seasonEndStrengthChange(16, 18));
        assertEquals(-3, SeasonEndService.seasonEndStrengthChange(18, 18));
    }

    @Test
    void sonUcTakimDuserDortBuyuklerDusmezBirUsttekiDuser() {
        List<StandingResponse> standings = standings(18);

        assertEquals(List.of(18L, 17L, 16L), StandingsService.relegationZone(standings, Set.of()));
        // 17. sıradaki takım 4 büyüklerden: onun yerine 15. düşer
        assertEquals(List.of(18L, 16L, 15L), StandingsService.relegationZone(standings, Set.of(17L)));
    }

    @Test
    void kucukLigdeKumeDusmeYok() {
        assertEquals(List.of(), StandingsService.relegationZone(standings(6), Set.of()));
    }

    private static List<StandingResponse> standings(int count) {
        return LongStream.rangeClosed(1, count)
                .mapToObj(id -> new StandingResponse((int) id, id, "Takim " + id, 0, 0, 0, 0, 0, 0, 0, 0, 0))
                .toList();
    }
}
