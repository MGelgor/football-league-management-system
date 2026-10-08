package com.footballleague.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Component;

import com.footballleague.entity.Player;
import com.footballleague.entity.Position;
import com.footballleague.entity.Team;

/**
 * Takım kadrosu üretir: kullanıcının eklediği oyuncular korunur, şablondaki
 * (2 KL, 6 DEF, 6 OS, 4 FV) eksik mevkiler rastgele isim, forma no ve güçle tamamlanır.
 */
@Component
public class SquadGenerator {

    static final Map<Position, Integer> SQUAD_TEMPLATE = new EnumMap<>(Map.of(
            Position.GOALKEEPER, 2,
            Position.DEFENDER, 6,
            Position.MIDFIELDER, 6,
            Position.FORWARD, 4));

    private static final List<String> FIRST_NAMES = List.of(
            "Ahmet", "Mehmet", "Mustafa", "Emre", "Burak", "Kerem", "Arda", "Can", "Cenk", "Hakan",
            "Oğuz", "Selçuk", "Volkan", "Ozan", "Yusuf", "Enes", "Berkay", "Kaan", "Barış", "Serdar",
            "Mert", "Okan", "Tolga", "Umut", "Ferdi", "İsmail", "Orkun", "Salih", "Taylan", "Eren",
            "Furkan", "Halil", "Onur", "Gökhan", "Cengiz", "Doğukan", "Yunus", "Kenan", "Altay", "Uğurcan");

    private static final List<String> LAST_NAMES = List.of(
            "Yılmaz", "Kaya", "Demir", "Şahin", "Çelik", "Yıldız", "Yıldırım", "Öztürk", "Aydın", "Özdemir",
            "Arslan", "Doğan", "Kılıç", "Aslan", "Çetin", "Kara", "Koç", "Kurt", "Özkan", "Şimşek",
            "Polat", "Erdoğan", "Güneş", "Aksoy", "Tekin", "Bulut", "Ünal", "Toprak", "Korkmaz", "Turan",
            "Akın", "Bayram", "Uçar", "Tosun", "Karaca", "Sarı", "Keskin", "Ateş", "Yazıcı", "Güler");

    private static final int MAX_SHIRT_NUMBER = 99;
    // Rastgele oyuncunun gücü takım gücünün etrafında dağılır
    private static final int STRENGTH_SPREAD = 15;
    // Gençlerden gelen oyuncu takım gücünün biraz altında başlar
    private static final int YOUTH_STRENGTH_GAP = 20;

    public List<Player> generate(Team team) {
        return complete(team, List.of());
    }

    /** given: kullanıcının eklediği oyuncular (team alanı set edilmiş). Dönen liste given + üretilenler. */
    public List<Player> complete(Team team, List<Player> given) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Set<String> usedNames = given.stream().map(Player::getName).collect(Collectors.toCollection(HashSet::new));
        Set<Integer> usedNumbers = given.stream().map(Player::getShirtNumber).collect(Collectors.toSet());
        Map<Position, Long> countByPosition = given.stream()
                .collect(Collectors.groupingBy(Player::getPosition, Collectors.counting()));

        List<Integer> freeNumbers = new ArrayList<>(IntStream.rangeClosed(1, MAX_SHIRT_NUMBER).boxed()
                .filter(number -> !usedNumbers.contains(number))
                .toList());
        Collections.shuffle(freeNumbers, random);
        // 1 numara boştaysa ilk üretilen oyuncuya verilir (şablonda önce kaleciler üretilir)
        if (freeNumbers.remove(Integer.valueOf(1))) {
            freeNumbers.addFirst(1);
        }

        List<Player> squad = new ArrayList<>(given);
        for (Position position : SQUAD_TEMPLATE.keySet()) {
            long missing = SQUAD_TEMPLATE.get(position) - countByPosition.getOrDefault(position, 0L);
            for (int i = 0; i < missing; i++) {
                squad.add(Player.builder()
                        .team(team)
                        .name(uniqueName(usedNames, random))
                        .position(position)
                        .shirtNumber(freeNumbers.removeFirst())
                        .strength(randomStrength(team, random))
                        .age(randomAge(random))
                        .build());
            }
        }
        return squad;
    }

    /** Emekli olan oyuncunun yerine aynı mevkide 17-19 yaşında genç oyuncu. */
    public Player youthPlayer(Team team, Position position, Set<String> usedNames, Set<Integer> usedNumbers) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<Integer> freeNumbers = IntStream.rangeClosed(2, MAX_SHIRT_NUMBER).boxed()
                .filter(number -> !usedNumbers.contains(number))
                .toList();
        int center = (team.getStrength() != null ? team.getStrength() : 60) - YOUTH_STRENGTH_GAP;
        return Player.builder()
                .team(team)
                .name(uniqueName(usedNames, random))
                .position(position)
                .shirtNumber(freeNumbers.get(random.nextInt(freeNumbers.size())))
                .strength(Math.clamp(center + random.nextInt(-10, 11), Player.MIN_STRENGTH, Player.MAX_STRENGTH))
                .age(random.nextInt(17, 20))
                .build();
    }

    /** Altyapıdan gelen 16-18 yaşında oyuncu (takım gücünün 20 altı civarında). */
    public Player academyPlayer(Team team, Position position, Set<String> usedNames, Set<Integer> usedNumbers) {
        Player player = youthPlayer(team, position, usedNames, usedNumbers);
        player.setAge(ThreadLocalRandom.current().nextInt(16, 19));
        return player;
    }

    /** Takımsız serbest oyuncu (forma numarası 0; imzalayınca takımda boş numara alır). */
    public Player freeAgent(Position position, Set<String> usedNames) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return Player.builder()
                .name(uniqueName(usedNames, random))
                .position(position)
                .shirtNumber(0)
                .strength(random.nextInt(35, 81))
                .age(random.nextInt(21, 34))
                .build();
    }

    /** Takımda kullanılmayan en küçük forma numarası (1 kalecilere bırakılır). */
    public static int freeShirtNumber(Set<Integer> usedNumbers, Position position) {
        int start = position == Position.GOALKEEPER && !usedNumbers.contains(1) ? 1 : 2;
        for (int number = start; number <= MAX_SHIRT_NUMBER; number++) {
            if (!usedNumbers.contains(number)) {
                return number;
            }
        }
        throw new IllegalStateException("Boş forma numarası yok");
    }

    /** 18-34 arası, 26 civarında yoğunlaşan (üçgen dağılım) yaş. */
    private static int randomAge(ThreadLocalRandom random) {
        return 18 + random.nextInt(0, 9) + random.nextInt(0, 9);
    }

    private static int randomStrength(Team team, ThreadLocalRandom random) {
        int center = team.getStrength() != null ? team.getStrength() : 60;
        return Math.clamp(center + random.nextInt(-STRENGTH_SPREAD, STRENGTH_SPREAD + 1),
                Player.MIN_STRENGTH, Player.MAX_STRENGTH);
    }

    /** Teknik direktör vb. için kullanılmamış rastgele ad (usedNames'e eklenir). */
    public String personName(Set<String> usedNames) {
        return uniqueName(usedNames, ThreadLocalRandom.current());
    }

    private String uniqueName(Set<String> usedNames, ThreadLocalRandom random) {
        String name;
        do {
            name = FIRST_NAMES.get(random.nextInt(FIRST_NAMES.size())) + " "
                    + LAST_NAMES.get(random.nextInt(LAST_NAMES.size()));
        } while (!usedNames.add(name));
        return name;
    }
}
