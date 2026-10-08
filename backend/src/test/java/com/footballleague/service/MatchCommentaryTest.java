package com.footballleague.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.footballleague.entity.MatchEventType;

class MatchCommentaryTest {

    @Test
    void isimEkleriUnluUyumunaGoreUretilir() {
        assertEquals("Mehmet Kaya'nın", MatchCommentary.genitive("Mehmet Kaya"));
        assertEquals("Emre Demir'in", MatchCommentary.genitive("Emre Demir"));
        assertEquals("Ozan Kurt'un", MatchCommentary.genitive("Ozan Kurt"));
        assertEquals("Arda Öztürk'ün", MatchCommentary.genitive("Arda Öztürk"));
        assertEquals("Can Koç'un", MatchCommentary.genitive("Can Koç"));
        assertEquals("Mehmet Kaya'ya", MatchCommentary.dative("Mehmet Kaya"));
        assertEquals("Emre Demir'e", MatchCommentary.dative("Emre Demir"));
        assertEquals("Okan Güler'e", MatchCommentary.dative("Okan Güler"));
        assertEquals("Umut Turan'a", MatchCommentary.dative("Umut Turan"));
    }

    @Test
    void herOlayTuruIcinOyuncuAdiniIcerenCumleUretilirVeAyniOlayHepAyniCumleyiAlir() {
        for (MatchEventType type : MatchEventType.values()) {
            for (int minute = 1; minute <= 90; minute++) {
                String text = MatchCommentary.describe(type, minute, "Ali Veli", "Can Kara", "Takım FK", false);
                assertTrue(text.contains("Ali Veli"), text);
                assertEquals(text, MatchCommentary.describe(type, minute, "Ali Veli", "Can Kara", "Takım FK", false));
            }
        }
        assertTrue(MatchCommentary.describe(MatchEventType.GOAL, 10, "Ali Veli", null, "X", false).startsWith("GOL!"));
        assertTrue(MatchCommentary.describe(MatchEventType.GOAL, 10, "Ali Veli", null, "X", true).contains("enaltı"));
        assertTrue(MatchCommentary.describe(MatchEventType.RED_CARD, 10, "Ali Veli", null, "Takım FK", false)
                .contains("Takım FK"));
    }
}
