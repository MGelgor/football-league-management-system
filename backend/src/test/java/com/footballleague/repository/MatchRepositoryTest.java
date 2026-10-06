package com.footballleague.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.footballleague.entity.Match;
import com.footballleague.entity.MatchWeek;
import com.footballleague.entity.Season;
import com.footballleague.entity.Team;

@DataJpaTest
class MatchRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MatchRepository matchRepository;

    private Season season;
    private Team a;
    private Team b;
    private Team c;
    private Team d;

    @BeforeEach
    void setUp() {
        season = entityManager.persist(Season.builder().seasonNumber(1).build());
        a = entityManager.persist(team("A"));
        b = entityManager.persist(team("B"));
        c = entityManager.persist(team("C"));
        d = entityManager.persist(team("D"));
    }

    @Test
    void sezonunMaclariHaftaNumarasinaGoreSiraliVeTakimlarYuklenmisGelir() {
        // Hafta 2, hafta 1'den once kaydediliyor: siralama kayit sirasina degil hafta numarasina gore olmali
        MatchWeek week2 = entityManager.persist(week(season, 2));
        MatchWeek week1 = entityManager.persist(week(season, 1));
        Match week2Match = entityManager.persist(match(week2, a, b));
        Match week1First = entityManager.persist(match(week1, c, d));
        Match week1Second = entityManager.persist(match(week1, a, c));
        Season otherSeason = entityManager.persist(Season.builder().seasonNumber(2).build());
        entityManager.persist(match(entityManager.persist(week(otherSeason, 1)), b, d));
        entityManager.flush();
        entityManager.clear();

        List<Match> matches = matchRepository.findLeagueMatchesBySeason(season.getId());

        assertEquals(List.of(week1First.getId(), week1Second.getId(), week2Match.getId()),
                matches.stream().map(Match::getId).toList(), "Diger sezonun maci gelmemeli");
        assertTrue(matches.stream().allMatch(match -> Hibernate.isInitialized(match.getHomeTeam())
                && Hibernate.isInitialized(match.getAwayTeam())
                && Hibernate.isInitialized(match.getMatchWeek())),
                "join fetch sayesinde takimlar ve hafta ek sorgu olmadan yuklenmis olmali");
    }

    @Test
    void ilkOynanmamisHaftaBulunurHepsiOynanincaBosDoner() {
        MatchWeek week1 = entityManager.persist(week(season, 1));
        MatchWeek week2 = entityManager.persist(week(season, 2));
        Match first = match(week1, a, b);
        first.setHomeScore(1);
        first.setAwayScore(0);
        entityManager.persist(first);
        Match second = entityManager.persist(match(week2, c, d));

        assertEquals(Optional.of(2), matchRepository.findFirstUnplayedWeekNumber(season.getId()));

        second.setHomeScore(0);
        second.setAwayScore(0);
        entityManager.flush();

        assertEquals(Optional.empty(), matchRepository.findFirstUnplayedWeekNumber(season.getId()));
    }

    @Test
    void sezonSilinirkenSadeceOSezonunMaclariSilinir() {
        Season otherSeason = entityManager.persist(Season.builder().seasonNumber(2).build());
        entityManager.persist(match(entityManager.persist(week(season, 1)), a, b));
        Match kept = entityManager.persist(match(entityManager.persist(week(otherSeason, 1)), c, d));
        entityManager.flush();

        matchRepository.deleteBySeasonId(season.getId());

        assertEquals(List.of(kept.getId()), matchRepository.findAll().stream().map(Match::getId).toList());
    }

    private Team team(String name) {
        return Team.builder().name(name).foundedYear(1900).colors("Mavi").strength(50).morale(50).build();
    }

    private MatchWeek week(Season weekSeason, int number) {
        return MatchWeek.builder().season(weekSeason).weekNumber(number).build();
    }

    private Match match(MatchWeek week, Team home, Team away) {
        return Match.builder().matchWeek(week).homeTeam(home).awayTeam(away).build();
    }
}
