package com.footballleague.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.footballleague.entity.Player;
import com.footballleague.entity.Position;
import com.footballleague.entity.Team;

class TransferRulesTest {

    private final SquadGenerator squadGenerator = new SquadGenerator();

    @Test
    void enGucluUcOyuncuDahaPahaliSatilir() {
        List<Player> squad = squad();
        Player star = squad.stream().max((a, b) -> a.getStrength() - b.getStrength()).orElseThrow();
        Player bench = squad.stream().min((a, b) -> a.getStrength() - b.getStrength()).orElseThrow();

        assertEquals(Math.round(Economy.marketValue(star) * 1.4 / 10_000) * 10_000, TransferService.askingPrice(star, squad));
        assertEquals(Math.round(Economy.marketValue(bench) * 1.1 / 10_000) * 10_000, TransferService.askingPrice(bench, squad));
    }

    @Test
    void kadro18inVeyaMevkiSablonununAltinaDusecekseSatilmaz() {
        List<Player> squad = squad();
        Player forward = squad.stream().filter(p -> p.getPosition() == Position.FORWARD).findFirst().orElseThrow();
        assertFalse(TransferService.canSell(squad, forward), "18 kişilik kadrodan satış yok");

        squad.add(Player.builder().name("Yedek Forvet").position(Position.FORWARD).strength(50).age(25).build());
        assertTrue(TransferService.canSell(squad, forward), "Fazla forvet varken satılabilir");
        Player goalkeeper = squad.stream().filter(p -> p.getPosition() == Position.GOALKEEPER).findFirst().orElseThrow();
        assertFalse(TransferService.canSell(squad, goalkeeper), "Kaleci şablonu (2) bozulur");
    }

    private List<Player> squad() {
        Team team = Team.builder().id(1L).name("A").strength(60).build();
        return new ArrayList<>(squadGenerator.generate(team));
    }
}
