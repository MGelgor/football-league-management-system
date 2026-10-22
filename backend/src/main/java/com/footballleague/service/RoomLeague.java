package com.footballleague.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.footballleague.entity.CupRound;
import com.footballleague.entity.Match;
import com.footballleague.entity.Season;
import com.footballleague.exception.FixtureNotGeneratedException;
import com.footballleague.repository.MatchRepository;
import com.footballleague.repository.SeasonRepository;

import lombok.RequiredArgsConstructor;

/**
 * Bir odanın ligini zamanlayıcı adına ilerletir (RoomContext dolu iken çağrılır). Ne oynanacağı ligin
 * durumundan çıkarılır: sıradaki oynanmamış hafta, lig bittiyse sıradaki kupa turu. Böylece aynı olay iki kez
 * oynanmaz. Kendi transaction'ı yoktur; çağırdığı servisler kendi transaction'larını açar.
 */
@Service
@RequiredArgsConstructor
public class RoomLeague {

    private static final Map<CupRound, String> CUP_ROUND_NAMES = Map.of(
            CupRound.QUARTER_FINAL, "Kupa çeyrek finali",
            CupRound.SEMI_FINAL, "Kupa yarı finali",
            CupRound.FINAL, "Kupa finali");

    private final SeasonRepository seasonRepository;
    private final MatchRepository matchRepository;
    private final FixtureService fixtureService;
    private final MatchSimulationService matchSimulationService;
    private final CupService cupService;

    /** Yeni sezonun fikstürü; transfer penceresi de bununla kapanır. */
    public void startSeason() {
        fixtureService.generateFixture();
    }

    /** week: oynanan hafta (kupa 101+); seasonComplete: lig + kupa tamamlandı. */
    public record MatchDay(int week, boolean seasonComplete) {
    }

    /** Bir maç günü: sıradaki lig haftası ya da kupa turu. */
    public MatchDay playMatchDay() {
        Season season = latestSeason().orElseThrow(FixtureNotGeneratedException::new);
        int week;
        if (!season.isFinished()) {
            week = matchRepository.findFirstUnplayedWeekNumber(season.getId()).orElseThrow();
            matchSimulationService.playWeek(week, true);
        } else {
            if (matchRepository.findCupMatchesBySeason(season.getId()).isEmpty()) {
                cupService.startCup();
            }
            week = matchRepository.findCupMatchesBySeason(season.getId()).stream().filter(match -> !match.isPlayed())
                    .findFirst().orElseThrow().getMatchWeek().getWeekNumber();
            cupService.playRound(true);
        }
        return new MatchDay(week, seasonComplete());
    }

    /** "5. hafta" ya da kupa turu adı (hafta 101+). */
    public static String weekTitle(int week) {
        return week > CupService.CUP_WEEK_OFFSET
                ? CUP_ROUND_NAMES.get(CupRound.values()[week - CupService.CUP_WEEK_OFFSET - 1])
                : week + ". hafta";
    }

    public boolean seasonComplete() {
        return latestSeason().map(season -> season.isFinished() && season.getCupWinner() != null).orElse(false);
    }

    /** Sıradaki maç gününde ne oynanacak (ör. "5. hafta", "Kupa finali"). */
    public String nextMatchDay() {
        Optional<Season> latest = latestSeason();
        if (latest.isEmpty() || seasonComplete()) {
            return "Yeni sezon";
        }
        Season season = latest.get();
        if (!season.isFinished()) {
            return matchRepository.findFirstUnplayedWeekNumber(season.getId()).orElse(1) + ". hafta";
        }
        List<Match> cup = matchRepository.findCupMatchesBySeason(season.getId());
        CupRound round = cup.stream().filter(match -> !match.isPlayed()).findFirst()
                .map(match -> match.getMatchWeek().getCupRound())
                .orElse(CupRound.QUARTER_FINAL);
        return CUP_ROUND_NAMES.get(round);
    }

    public Optional<Integer> seasonNumber() {
        return latestSeason().map(Season::getSeasonNumber);
    }

    private Optional<Season> latestSeason() {
        return seasonRepository.findTopByOrderBySeasonNumberDesc();
    }
}
