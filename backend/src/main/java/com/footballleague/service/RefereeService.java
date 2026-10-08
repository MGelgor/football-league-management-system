package com.footballleague.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.footballleague.dto.RefereeResponse;
import com.footballleague.entity.Match;
import com.footballleague.entity.MatchEventType;
import com.footballleague.entity.Referee;
import com.footballleague.repository.RefereeRepository;

import lombok.RequiredArgsConstructor;

/** Hakem havuzu: uygulama açılışında oluşturulur, maçlara her hafta (aynı haftada bir hakeme bir maç) atanır. */
@Service
@RequiredArgsConstructor
@Transactional
public class RefereeService {

    // Kurgusal adlar
    static final List<String> NAMES = List.of(
            "Selim Arıkan", "Kemal Duran", "Tarık Ergin", "Necati Bozkurt", "Levent Soylu", "Orhan Yaman",
            "Sinan Akbaş", "Reşat Uyar", "Erkan Çakır", "Fikret Sezer", "Nail Gürsoy", "Rıza Tamer", "Zafer Okur",
            "Haluk Ilgaz");

    private final RefereeRepository refereeRepository;

    public void ensureReferees() {
        if (refereeRepository.count() > 0) {
            return;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        refereeRepository.saveAll(NAMES.stream()
                .map(name -> Referee.builder().name(name).strictness(random.nextInt(2, 10)).build())
                .toList());
    }

    /** Aynı haftanın maçlarına farklı hakemler (havuz yetmezse tekrar edebilir). */
    public void assign(List<Match> weekMatches) {
        List<Referee> pool = new ArrayList<>(refereeRepository.findAll());
        if (pool.isEmpty()) {
            return;
        }
        Collections.shuffle(pool, ThreadLocalRandom.current());
        for (int i = 0; i < weekMatches.size(); i++) {
            weekMatches.get(i).setReferee(pool.get(i % pool.size()));
        }
    }

    /** En sert hakem başta. */
    @Transactional(readOnly = true)
    public List<RefereeResponse> getReferees() {
        Map<Long, Long> matches = new HashMap<>();
        refereeRepository.countPlayedMatches().forEach(row -> matches.put((Long) row[0], (Long) row[1]));
        Map<Long, long[]> counts = new HashMap<>();
        for (Object[] row : refereeRepository.countEvents()) {
            long[] values = counts.computeIfAbsent((Long) row[0], id -> new long[3]);
            MatchEventType type = (MatchEventType) row[1];
            long count = (Long) row[3];
            if (type == MatchEventType.YELLOW_CARD) {
                values[0] += count;
            } else if (type == MatchEventType.RED_CARD) {
                values[1] += count;
            } else if ((Boolean) row[2]) {
                values[2] += count;
            }
        }
        return refereeRepository.findAll().stream()
                .map(referee -> {
                    long played = matches.getOrDefault(referee.getId(), 0L);
                    long[] values = counts.getOrDefault(referee.getId(), new long[3]);
                    return new RefereeResponse(referee.getId(), referee.getName(), referee.getStrictness(), played,
                            values[0], values[1], values[2], perMatch(values[0], played), perMatch(values[1], played));
                })
                .sorted(Comparator.comparingInt(RefereeResponse::strictness).reversed()
                        .thenComparing(RefereeResponse::name))
                .toList();
    }

    private static double perMatch(long count, long matches) {
        return matches == 0 ? 0 : Math.round(count * 100.0 / matches) / 100.0;
    }
}
