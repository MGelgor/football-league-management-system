package com.footballleague.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.footballleague.dto.MatchResponse;
import com.footballleague.dto.MatchWeekResponse;
import com.footballleague.entity.Competition;
import com.footballleague.entity.Match;
import com.footballleague.entity.MatchAppearance;
import com.footballleague.entity.MatchEvent;
import com.footballleague.entity.MatchEventType;
import com.footballleague.entity.MatchWeek;
import com.footballleague.entity.Player;
import com.footballleague.entity.Position;
import com.footballleague.entity.Season;
import com.footballleague.entity.Team;
import com.footballleague.exception.FixtureNotGeneratedException;
import com.footballleague.exception.MatchWeekNotFoundException;
import com.footballleague.exception.WeekAlreadyPlayedException;
import com.footballleague.exception.WeekOrderException;
import com.footballleague.repository.MatchAppearanceRepository;
import com.footballleague.repository.MatchEventRepository;
import com.footballleague.repository.MatchRepository;
import com.footballleague.repository.MatchTeamStatsRepository;
import com.footballleague.repository.MatchWeekRepository;
import com.footballleague.repository.PlayerRepository;
import com.footballleague.repository.SeasonRepository;
import com.footballleague.repository.TeamRepository;
import com.footballleague.service.ScoreSimulator.ExpectedGoals;
import com.footballleague.service.ScoreSimulator.Probabilities;
import com.footballleague.service.ScoreSimulator.SimulatedScore;

@ExtendWith(MockitoExtension.class)
class MatchSimulationServiceTest {

    private static final Long SEASON_ID = 3L;
    private static final Long WEEK_ID = 10L;
    private static final ExpectedGoals EXPECTED = new ExpectedGoals(1.3, 1.3);
    // Esit guc: beklenen puan 3*0.4 + 0.2 = 1.4
    private static final Probabilities EVEN = new Probabilities(0.4, 0.2, 0.4);

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private MatchWeekRepository matchWeekRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private MatchEventRepository matchEventRepository;

    @Mock
    private MatchTeamStatsRepository matchTeamStatsRepository;

    @Mock
    private MatchAppearanceRepository matchAppearanceRepository;

    @Mock
    private ScoreSimulator scoreSimulator;

    @Mock
    private SeasonEndService seasonEndService;

    private MatchSimulationService matchSimulationService;

    private final Season season = Season.builder().id(SEASON_ID).seasonNumber(1).build();

    @BeforeEach
    void setUp() {
        // Mac detayi uretici gercek: kadrosu olmayan takimlarda olay uretmez, sadece istatistik uretir
        matchSimulationService = new MatchSimulationService(seasonRepository, matchWeekRepository, matchRepository,
                teamRepository, playerRepository, matchEventRepository, matchTeamStatsRepository,
                matchAppearanceRepository, scoreSimulator, new MatchDetailGenerator(),
                new MatchMapper(new ScoreSimulator()), seasonEndService);
    }

    @Test
    void fiksturYoksaHaftaOynatilamaz() {
        when(seasonRepository.findTopByOrderBySeasonNumberDesc()).thenReturn(Optional.empty());

        assertThrows(FixtureNotGeneratedException.class, () -> matchSimulationService.playWeek(1));
    }

    @Test
    void olmayanHaftaVeKupaHaftasiLigHaftasiOlarakOynatilamaz() {
        when(seasonRepository.findTopByOrderBySeasonNumberDesc()).thenReturn(Optional.of(season));
        when(matchWeekRepository.findBySeasonIdAndWeekNumber(SEASON_ID, 99)).thenReturn(Optional.empty());
        when(matchWeekRepository.findBySeasonIdAndWeekNumber(SEASON_ID, 101)).thenReturn(Optional.of(
                MatchWeek.builder().id(50L).weekNumber(101).competition(Competition.CUP).build()));

        assertThrows(MatchWeekNotFoundException.class, () -> matchSimulationService.playWeek(99));
        assertThrows(MatchWeekNotFoundException.class, () -> matchSimulationService.playWeek(101));
    }

    @Test
    void oynanmisHaftaTekrarOynatilamaz() {
        Match played = match(team(1L, 50, 50), team(2L, 50, 50));
        played.setHomeScore(1);
        played.setAwayScore(0);
        givenWeekWithMatches(1, played);

        assertThrows(WeekAlreadyPlayedException.class, () -> matchSimulationService.playWeek(1));
        verifyNoInteractions(scoreSimulator);
    }

    @Test
    void oncekiHaftaOynanmadanSonrakiHaftaOynatilamaz() {
        givenWeekWithMatches(5, match(team(1L, 50, 50), team(2L, 50, 50)));
        when(matchRepository.findFirstUnplayedWeekNumber(SEASON_ID)).thenReturn(Optional.of(1));

        WeekOrderException exception =
                assertThrows(WeekOrderException.class, () -> matchSimulationService.playWeek(5));
        assertTrue(exception.getMessage().contains("sıradaki hafta 1"), exception.getMessage());
        verifyNoInteractions(scoreSimulator);
    }

    @Test
    void simulatoreTakimlarinMacGucuVeMoraliGonderilirMacOncesiOlasiliklarKaydedilir() {
        Team bigFour = team(1L, 80, 70);
        bigFour.setBigFour(true);
        Match match = match(bigFour, team(2L, 40, 30));
        givenPlayableWeekOne(match);
        int bigFourMatchStrength = 80 + Team.BIG_FOUR_MATCH_BONUS;
        when(scoreSimulator.expectedGoals(bigFourMatchStrength, 70, 40, 30)).thenReturn(EXPECTED);
        when(scoreSimulator.probabilities(EXPECTED)).thenReturn(new Probabilities(0.62, 0.21, 0.17));
        when(scoreSimulator.simulate(EXPECTED)).thenReturn(new SimulatedScore(0, 0));

        matchSimulationService.playWeek(1);

        assertEquals(62, match.getHomeWinProbability());
        assertEquals(21, match.getDrawProbability());
        assertEquals(17, match.getAwayWinProbability());
    }

    @Test
    void skorlarKaydedilirVeMorallerSonucaGoreDegisir() {
        Team homeWinner = team(1L, 50, 50);
        Team awayLoser = team(2L, 50, 50);
        Team homeLoser = team(3L, 50, 50);
        Team awayWinner = team(4L, 50, 50);
        Team drawHome = team(5L, 50, 50);
        Team drawAway = team(6L, 50, 50);
        Match homeWin = match(homeWinner, awayLoser);
        Match awayWin = match(homeLoser, awayWinner);
        Match draw = match(drawHome, drawAway);
        givenPlayableWeekOne(homeWin, awayWin, draw);
        givenEvenSimulator(new SimulatedScore(3, 1), new SimulatedScore(0, 2), new SimulatedScore(2, 2));

        MatchWeekResponse response = matchSimulationService.playWeek(1);

        assertEquals(3, homeWin.getHomeScore());
        assertEquals(1, homeWin.getAwayScore());
        assertTrue(response.matches().stream().allMatch(MatchResponse::played));
        verify(matchRepository).saveAll(List.of(homeWin, awayWin, draw));

        assertEquals(60, homeWinner.getMorale(), "Galibiyet +10");
        assertEquals(40, awayLoser.getMorale(), "Maglubiyet -10");
        assertEquals(40, homeLoser.getMorale(), "Evinde kaybeden -10");
        assertEquals(60, awayWinner.getMorale(), "Deplasmanda kazanan +10");
        assertEquals(50, drawHome.getMorale(), "Beraberlikte moral degismez");
        assertEquals(50, drawAway.getMorale(), "Beraberlikte moral degismez");
    }

    @Test
    void gucBeklentiyeGoreDegisirVeSonDegisimKaydedilir() {
        Team winner = team(1L, 50, 50);
        Team loser = team(2L, 50, 50);
        givenPlayableWeekOne(match(winner, loser));
        givenEvenSimulator(new SimulatedScore(2, 0));

        matchSimulationService.playWeek(1);

        // (3 - 1.4) * 0.7 = 1.12 -> +1 ; (0 - 1.4) * 0.7 = -0.98 -> -1
        assertEquals(51, winner.getStrength());
        assertEquals(1, winner.getLastStrengthChange());
        assertEquals(49, loser.getStrength());
        assertEquals(-1, loser.getLastStrengthChange());
    }

    @Test
    void kupaMaclariMoraliVeGucuDegistirmez() {
        Team winner = team(1L, 50, 50);
        Team loser = team(2L, 50, 50);
        Match cupMatch = match(winner, loser);
        when(playerRepository.findByTeamIdInAndActiveTrue(anyList())).thenReturn(List.of());
        givenEvenSimulator(new SimulatedScore(3, 0));

        matchSimulationService.simulateMatches(List.of(cupMatch), Competition.CUP);

        assertEquals(3, cupMatch.getHomeScore());
        assertEquals(50, winner.getMorale());
        assertEquals(50, winner.getStrength());
        assertEquals(50, loser.getStrength());
    }

    @Test
    void moral0Ile100ArasindaKalir() {
        Team highMorale = team(1L, 50, 100);
        Team lowMorale = team(2L, 50, 0);
        givenPlayableWeekOne(match(highMorale, lowMorale));
        givenEvenSimulator(new SimulatedScore(2, 0));

        matchSimulationService.playWeek(1);

        assertEquals(100, highMorale.getMorale());
        assertEquals(0, lowMorale.getMorale());
    }

    @Test
    void sonHaftaOynaninaSezonSonuIslemleriCalisir() {
        givenPlayableWeekOne(match(team(1L, 50, 50), team(2L, 50, 50)));
        givenEvenSimulator(new SimulatedScore(1, 0));
        when(matchRepository.findFirstUnplayedWeekNumber(SEASON_ID)).thenReturn(Optional.of(1), Optional.empty());

        matchSimulationService.playWeek(1);

        verify(seasonEndService).finishSeason(season);
    }

    @Test
    void sezonOrtasindaSezonBitmez() {
        givenPlayableWeekOne(match(team(1L, 50, 50), team(2L, 50, 50)));
        givenEvenSimulator(new SimulatedScore(1, 1));
        when(matchRepository.findFirstUnplayedWeekNumber(SEASON_ID)).thenReturn(Optional.of(1), Optional.of(2));

        matchSimulationService.playWeek(1);

        verify(seasonEndService, never()).finishSeason(any());
    }

    @Test
    void kirmiziKartBirMacCezaDortSariBirMacCezaSakatlikBirIleUcMacArasi() {
        Team team = team(1L, 50, 50);
        Player red = player(1L, team);
        Player fourthYellow = player(2L, team);
        fourthYellow.setSeasonYellowCards(3);
        Player thirdYellow = player(3L, team);
        thirdYellow.setSeasonYellowCards(1);
        Player injured = player(4L, team);

        MatchSimulationService.updatePlayerStatuses(List.of(red, fourthYellow, thirdYellow, injured), List.of(
                event(red, MatchEventType.RED_CARD),
                event(fourthYellow, MatchEventType.YELLOW_CARD),
                event(thirdYellow, MatchEventType.YELLOW_CARD),
                event(injured, MatchEventType.INJURY)));

        assertEquals(1, red.getSuspendedMatches());
        assertEquals(1, fourthYellow.getSuspendedMatches(), "4. sari -> 1 mac ceza");
        assertEquals(0, thirdYellow.getSuspendedMatches(), "2. sari ceza getirmez");
        assertTrue(injured.getInjuredMatches() >= 1 && injured.getInjuredMatches() <= 3);
    }

    @Test
    void cezaliVeSakatOyuncununKalanMacSayisiKacirdigiMactanSonraAzalir() {
        Team team = team(1L, 50, 50);
        Player suspended = player(1L, team);
        suspended.setSuspendedMatches(1);
        Player injured = player(2L, team);
        injured.setInjuredMatches(2);

        MatchSimulationService.updatePlayerStatuses(List.of(suspended, injured), List.of());

        assertEquals(0, suspended.getSuspendedMatches());
        assertTrue(suspended.isAvailable(), "Cezasini cektikten sonra oynayabilir");
        assertEquals(1, injured.getInjuredMatches());
    }

    @Test
    void cezaliVeSakatOyuncularKadroyaAlinmaz() {
        Team home = team(1L, 50, 50);
        Team away = team(2L, 50, 50);
        List<Player> homeSquad = new ArrayList<>(new SquadGenerator().generate(home));
        for (int i = 0; i < homeSquad.size(); i++) {
            homeSquad.get(i).setId(100L + i);
        }
        Player suspended = homeSquad.get(0);
        suspended.setSuspendedMatches(1);
        Player injured = homeSquad.get(1);
        injured.setInjuredMatches(2);
        when(playerRepository.findByTeamIdInAndActiveTrue(anyList())).thenReturn(homeSquad);
        givenEvenSimulator(new SimulatedScore(2, 0));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MatchAppearance>> appearances = ArgumentCaptor.forClass(List.class);

        matchSimulationService.simulateMatches(List.of(match(home, away)), Competition.LEAGUE);

        verify(matchAppearanceRepository).saveAll(appearances.capture());
        assertTrue(appearances.getValue().stream()
                .noneMatch(a -> a.getPlayer() == suspended || a.getPlayer() == injured));
        assertEquals(0, suspended.getSuspendedMatches(), "Kacirdigi mac cezadan duser");
        assertEquals(1, injured.getInjuredMatches());
    }

    private void givenEvenSimulator(SimulatedScore first, SimulatedScore... rest) {
        when(scoreSimulator.expectedGoals(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(EXPECTED);
        when(scoreSimulator.probabilities(EXPECTED)).thenReturn(EVEN);
        when(scoreSimulator.simulate(EXPECTED)).thenReturn(first, rest);
    }

    private void givenPlayableWeekOne(Match... matches) {
        givenWeekWithMatches(1, matches);
        when(playerRepository.findByTeamIdInAndActiveTrue(anyList())).thenReturn(List.of());
        lenient().when(matchRepository.findFirstUnplayedWeekNumber(SEASON_ID))
                .thenReturn(Optional.of(1), Optional.of(2));
    }

    private void givenWeekWithMatches(int weekNumber, Match... matches) {
        MatchWeek week = MatchWeek.builder().id(WEEK_ID).season(season).weekNumber(weekNumber).build();
        when(seasonRepository.findTopByOrderBySeasonNumberDesc()).thenReturn(Optional.of(season));
        when(matchWeekRepository.findBySeasonIdAndWeekNumber(SEASON_ID, weekNumber)).thenReturn(Optional.of(week));
        when(matchRepository.findByMatchWeekIdOrderById(WEEK_ID)).thenReturn(List.of(matches));
    }

    private static MatchEvent event(Player player, MatchEventType type) {
        return MatchEvent.builder().player(player).team(player.getTeam()).type(type).minute(10).build();
    }

    private static Player player(Long id, Team team) {
        return Player.builder().id(id).team(team).name("Oyuncu " + id).position(Position.MIDFIELDER)
                .shirtNumber(id.intValue()).strength(50).age(25).build();
    }

    private Match match(Team home, Team away) {
        return Match.builder().homeTeam(home).awayTeam(away).build();
    }

    private Team team(Long id, int strength, int morale) {
        return Team.builder().id(id).name("Takim " + id).strength(strength).morale(morale).build();
    }
}
