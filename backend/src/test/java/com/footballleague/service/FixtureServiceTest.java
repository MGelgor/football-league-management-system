package com.footballleague.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.footballleague.entity.Match;
import com.footballleague.entity.MatchWeek;
import com.footballleague.entity.Team;
import com.footballleague.exception.FixtureAlreadyGeneratedException;
import com.footballleague.repository.MatchRepository;
import com.footballleague.repository.MatchWeekRepository;
import com.footballleague.repository.TeamRepository;

@ExtendWith(MockitoExtension.class)
class FixtureServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private MatchWeekRepository matchWeekRepository;

    @Mock
    private MatchRepository matchRepository;

    @Captor
    private ArgumentCaptor<List<MatchWeek>> weeksCaptor;

    @Captor
    private ArgumentCaptor<List<Match>> matchesCaptor;

    private FixtureService fixtureService;

    @BeforeEach
    void setUp() {
        // Repository'ler sahte, fikstur algoritmasi gercek
        fixtureService = new FixtureService(teamRepository, matchWeekRepository, matchRepository,
                new RoundRobinScheduler());
    }

    @Test
    void fiksturZatenVarsaYenidenOlusturulmaz() {
        when(matchRepository.count()).thenReturn(306L);

        assertThrows(FixtureAlreadyGeneratedException.class, () -> fixtureService.generateFixture());
        verify(matchRepository, never()).saveAll(any());
    }

    @Test
    void enAz18TakimGerekir() {
        when(teamRepository.findAll()).thenReturn(teams(16));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> fixtureService.generateFixture());
        assertTrue(exception.getMessage().contains("en az 18"), exception.getMessage());
        verify(matchRepository, never()).saveAll(any());
    }

    @Test
    void takimSayisiCiftOlmalidir() {
        when(teamRepository.findAll()).thenReturn(teams(19));

        assertThrows(IllegalArgumentException.class, () -> fixtureService.generateFixture());
        verify(matchRepository, never()).saveAll(any());
    }

    @Test
    void onSekizTakimIcin34HaftaVe306MacKaydedilirRovanstaSahaTersCevrilir() {
        when(teamRepository.findAll()).thenReturn(teams(18));

        fixtureService.generateFixture();

        verify(matchWeekRepository).saveAll(weeksCaptor.capture());
        verify(matchRepository).saveAll(matchesCaptor.capture());
        List<MatchWeek> weeks = weeksCaptor.getValue();
        List<Match> matches = matchesCaptor.getValue();

        assertEquals(IntStream.rangeClosed(1, 34).boxed().toList(),
                weeks.stream().map(MatchWeek::getWeekNumber).toList());
        assertEquals(306, matches.size());
        assertTrue(matches.stream().noneMatch(Match::isPlayed), "Yeni fiksturde oynanmis mac olmamali");

        Map<Integer, Set<List<Long>>> pairingsByWeek = matches.stream().collect(Collectors.groupingBy(
                match -> match.getMatchWeek().getWeekNumber(),
                Collectors.mapping(match -> List.of(match.getHomeTeam().getId(), match.getAwayTeam().getId()),
                        Collectors.toSet())));

        for (int week = 1; week <= 17; week++) {
            Set<List<Long>> reversedFirstLeg = pairingsByWeek.get(week).stream()
                    .map(pairing -> List.of(pairing.get(1), pairing.get(0)))
                    .collect(Collectors.toSet());
            assertEquals(reversedFirstLeg, pairingsByWeek.get(week + 17),
                    "Hafta " + (week + 17) + ", hafta " + week + "'in ev sahibi/deplasman ters cevrilmis hali olmali");
        }
    }

    @Test
    void sifirlamaOnceMaclariSonraHaftalariSilerVeMoralleriSifirlar() {
        Team team = Team.builder().id(1L).morale(90).build();
        when(teamRepository.findAll()).thenReturn(List.of(team));

        fixtureService.resetFixture();

        InOrder inOrder = inOrder(matchRepository, matchWeekRepository);
        inOrder.verify(matchRepository).deleteAllInBatch();
        inOrder.verify(matchWeekRepository).deleteAllInBatch();
        assertEquals(Team.INITIAL_MORALE, team.getMorale());
    }

    private List<Team> teams(int count) {
        return LongStream.rangeClosed(1, count)
                .mapToObj(id -> Team.builder().id(id).name("Takim " + id).build())
                .toList();
    }
}
