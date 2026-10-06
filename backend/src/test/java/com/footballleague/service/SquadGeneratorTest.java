package com.footballleague.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.footballleague.entity.Player;
import com.footballleague.entity.Position;
import com.footballleague.entity.Team;

class SquadGeneratorTest {

    private final SquadGenerator squadGenerator = new SquadGenerator();

    @Test
    void kadro18KisiMevkiDagilimiDogruIsimVeFormaNumaralariTekrarsiz() {
        Team team = Team.builder().id(1L).name("Takim").build();

        List<Player> squad = squadGenerator.generate(team);

        assertEquals(18, squad.size());
        Map<Position, Long> countByPosition = squad.stream()
                .collect(Collectors.groupingBy(Player::getPosition, Collectors.counting()));
        assertEquals(Map.of(Position.GOALKEEPER, 2L, Position.DEFENDER, 6L, Position.MIDFIELDER, 6L,
                Position.FORWARD, 4L), countByPosition);
        assertEquals(18, squad.stream().map(Player::getName).distinct().count(), "Isimler tekrarsiz olmali");
        assertEquals(18, squad.stream().map(Player::getShirtNumber).distinct().count(), "Numaralar tekrarsiz olmali");
        assertTrue(squad.stream().allMatch(player -> player.getShirtNumber() >= 1 && player.getShirtNumber() <= 99));
        assertTrue(squad.stream().allMatch(player -> player.getTeam() == team));
        assertTrue(squad.stream().allMatch(player -> player.getStrength() >= 1 && player.getStrength() <= 100));

        Map<Integer, Player> byNumber = squad.stream()
                .collect(Collectors.toMap(Player::getShirtNumber, Function.identity()));
        assertEquals(Position.GOALKEEPER, byNumber.get(1).getPosition(), "1 numara kalecide olmali");
    }

    @Test
    void kullanicininOyunculariKorunurEksikMevkilerTamamlanirNumaraVeIsimCakismaz() {
        Team team = Team.builder().id(1L).name("Takim").strength(90).build();
        Player golcu = Player.builder().team(team).name("Kral Golcu").position(Position.FORWARD).shirtNumber(1).strength(100).build();
        Player forvet2 = Player.builder().team(team).name("Ikinci").position(Position.FORWARD).shirtNumber(10).strength(60).build();
        Player kaleci = Player.builder().team(team).name("Kaleci").position(Position.GOALKEEPER).shirtNumber(99).strength(70).build();

        List<Player> squad = squadGenerator.complete(team, List.of(golcu, forvet2, kaleci));

        assertEquals(18, squad.size());
        assertTrue(squad.containsAll(List.of(golcu, forvet2, kaleci)), "Eklenen oyuncular kadroda kalmali");
        Map<Position, Long> countByPosition = squad.stream()
                .collect(Collectors.groupingBy(Player::getPosition, Collectors.counting()));
        assertEquals(Map.of(Position.GOALKEEPER, 2L, Position.DEFENDER, 6L, Position.MIDFIELDER, 6L,
                Position.FORWARD, 4L), countByPosition);
        assertEquals(18, squad.stream().map(Player::getShirtNumber).distinct().count());
        assertEquals(18, squad.stream().map(Player::getName).distinct().count());
        assertTrue(squad.subList(3, squad.size()).stream()
                        .allMatch(player -> player.getStrength() >= 75 && player.getStrength() <= 100),
                "Rastgele oyuncularin gucu takim gucunun +-15 civarinda olmali");
    }

    @Test
    void sablondanFazlaOyuncuEklenirseHepsiKalirSadeceEksikMevkilerEklenir() {
        Team team = Team.builder().id(1L).name("Takim").strength(50).build();
        List<Player> forwards = IntStream.rangeClosed(1, 7)
                .mapToObj(i -> Player.builder().team(team).name("Forvet " + i).position(Position.FORWARD)
                        .shirtNumber(i + 10).strength(50).build())
                .toList();

        List<Player> squad = squadGenerator.complete(team, forwards);

        assertEquals(21, squad.size(), "7 forvet + 2 KL + 6 DEF + 6 OS");
    }
}
