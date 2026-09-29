package com.footballleague.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.footballleague.entity.Match;
import com.footballleague.entity.MatchWeek;
import com.footballleague.entity.Team;

@DataJpaTest
class MatchRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MatchRepository matchRepository;

    private Team a;
    private Team b;
    private Team c;
    private Team d;

    @BeforeEach
    void setUp() {
        a = entityManager.persist(team("A"));
        b = entityManager.persist(team("B"));
        c = entityManager.persist(team("C"));
        d = entityManager.persist(team("D"));
    }

    @Test
    void tumMaclarHaftaNumarasinaGoreSiraliVeTakimlarYuklenmisGelir() {
        // Hafta 2, hafta 1'den once kaydediliyor: siralama kayit sirasina degil hafta numarasina gore olmali
        MatchWeek week2 = entityManager.persist(MatchWeek.builder().weekNumber(2).build());
        MatchWeek week1 = entityManager.persist(MatchWeek.builder().weekNumber(1).build());
        Match week2Match = entityManager.persist(match(week2, a, b));
        Match week1First = entityManager.persist(match(week1, c, d));
        Match week1Second = entityManager.persist(match(week1, a, c));
        entityManager.flush();
        entityManager.clear();

        List<Match> matches = matchRepository.findAllWithTeamsOrderByWeek();

        assertEquals(List.of(week1First.getId(), week1Second.getId(), week2Match.getId()),
                matches.stream().map(Match::getId).toList());
        assertTrue(matches.stream().allMatch(match -> Hibernate.isInitialized(match.getHomeTeam())
                && Hibernate.isInitialized(match.getAwayTeam())
                && Hibernate.isInitialized(match.getMatchWeek())),
                "join fetch sayesinde takimlar ve hafta ek sorgu olmadan yuklenmis olmali");
    }

    @Test
    void sadeceIstenenHaftaninMaclariDoner() {
        MatchWeek week1 = entityManager.persist(MatchWeek.builder().weekNumber(1).build());
        MatchWeek week2 = entityManager.persist(MatchWeek.builder().weekNumber(2).build());
        Match first = entityManager.persist(match(week1, a, b));
        Match second = entityManager.persist(match(week1, c, d));
        entityManager.persist(match(week2, a, c));

        List<Match> week1Matches = matchRepository.findByMatchWeekIdOrderById(week1.getId());

        assertEquals(List.of(first.getId(), second.getId()), week1Matches.stream().map(Match::getId).toList());
    }

    private Team team(String name) {
        return Team.builder().name(name).foundedYear(1900).colors("Mavi").strength(50).morale(50).build();
    }

    private Match match(MatchWeek week, Team home, Team away) {
        return Match.builder().matchWeek(week).homeTeam(home).awayTeam(away).build();
    }
}
