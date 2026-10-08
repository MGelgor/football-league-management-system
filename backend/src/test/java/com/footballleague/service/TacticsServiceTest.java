package com.footballleague.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.footballleague.entity.Formation;
import com.footballleague.entity.Manager;
import com.footballleague.entity.PlayStyle;
import com.footballleague.entity.Team;

class TacticsServiceTest {

    private final ScoreSimulator scoreSimulator = new ScoreSimulator();
    private final TacticsService tacticsService = new TacticsService();

    @Test
    void yapayZekaZayifRakibeHucumGucluRakibeSavunmaOynarDengedeVarsayilanStil() {
        Team strong = team(80, PlayStyle.BALANCED);
        Team weak = team(50, PlayStyle.BALANCED);
        Team even = team(78, PlayStyle.DEFENSIVE);

        assertEquals(PlayStyle.ATTACKING, TacticsService.chooseStyle(strong, weak, true));
        assertEquals(PlayStyle.DEFENSIVE, TacticsService.chooseStyle(weak, strong, false));
        assertEquals(PlayStyle.DEFENSIVE, TacticsService.chooseStyle(even, strong, true), "Varsayılan stil");
    }

    @Test
    void hucumStiliIkiTarafinGolBeklentisiniArtirirSavunmaAzaltir() {
        ScoreSimulator.ExpectedGoals balanced = expected(PlayStyle.BALANCED, PlayStyle.BALANCED);
        ScoreSimulator.ExpectedGoals attacking = expected(PlayStyle.ATTACKING, PlayStyle.BALANCED);
        ScoreSimulator.ExpectedGoals defensive = expected(PlayStyle.DEFENSIVE, PlayStyle.BALANCED);

        assertTrue(attacking.home() > balanced.home() && attacking.away() > balanced.away());
        assertTrue(defensive.home() < balanced.home() && defensive.away() < balanced.away());

        // 5000 maçta ortalama gol de aynı yönde
        assertTrue(averageGoals(attacking) > averageGoals(balanced) + 0.1);
    }

    @Test
    void dizilisAvantajiVeUstaHocaGolPayiniArtirir() {
        ScoreSimulator.ExpectedGoals plain = scoreSimulator.expectedGoals(
                new ScoreSimulator.TeamSetup(60, 50, Formation.F442, PlayStyle.BALANCED, 0),
                new ScoreSimulator.TeamSetup(60, 50, Formation.F442, PlayStyle.BALANCED, 0));
        // 4-4-2, 4-3-3'ü yener
        ScoreSimulator.ExpectedGoals advantage = scoreSimulator.expectedGoals(
                new ScoreSimulator.TeamSetup(60, 50, Formation.F442, PlayStyle.BALANCED, 0),
                new ScoreSimulator.TeamSetup(60, 50, Formation.F433, PlayStyle.BALANCED, 0));
        ScoreSimulator.ExpectedGoals master = scoreSimulator.expectedGoals(
                new ScoreSimulator.TeamSetup(60, 50, Formation.F442, PlayStyle.BALANCED,
                        Manager.builder().tacticalSkill(100).build().strengthBonus()),
                new ScoreSimulator.TeamSetup(60, 50, Formation.F442, PlayStyle.BALANCED, 0));

        assertTrue(advantage.home() > plain.home() && advantage.away() < plain.away());
        assertTrue(master.home() > plain.home());
    }

    @Test
    void kurulumTakiminDizilisiniVeHocaBonusunuKullanir() {
        Team team = team(70, PlayStyle.BALANCED);
        team.setFormation(Formation.F352);
        team.setManager(Manager.builder().tacticalSkill(90).build());
        ScoreSimulator.TeamSetup setup = tacticsService.setup(team, team(70, PlayStyle.BALANCED), true);

        assertEquals(Formation.F352, setup.formation());
        assertEquals(2.0, setup.managerBonus());
        assertEquals(PlayStyle.BALANCED, setup.style());
    }

    private ScoreSimulator.ExpectedGoals expected(PlayStyle home, PlayStyle away) {
        return scoreSimulator.expectedGoals(new ScoreSimulator.TeamSetup(60, 50, Formation.F442, home, 0),
                new ScoreSimulator.TeamSetup(60, 50, Formation.F442, away, 0));
    }

    private double averageGoals(ScoreSimulator.ExpectedGoals expected) {
        int goals = 0;
        for (int i = 0; i < 5000; i++) {
            ScoreSimulator.SimulatedScore score = scoreSimulator.simulate(expected);
            goals += score.homeGoals() + score.awayGoals();
        }
        return goals / 5000.0;
    }

    private static Team team(int strength, PlayStyle style) {
        return Team.builder().strength(strength).morale(50).playStyle(style).build();
    }
}
