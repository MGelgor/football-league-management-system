package com.footballleague;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LeagueFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void dortBuyuklerHazirKadrolarDuzenlenebilirHaftalarSiraylaOynanirSezonlarArsivlenir() throws Exception {
        // 4 büyükler uygulama açılışında backend tarafından eklenmiş olmalı
        String teamsJson = mockMvc.perform(get("/api/teams"))
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[*].bigFour", everyItem(is(true))))
                .andExpect(jsonPath("$[*].strength", everyItem(greaterThanOrEqualTo(85))))
                .andReturn().getResponse().getContentAsString();
        int galatasarayId = first(teamsJson, "$[?(@.name == 'Galatasaray')].id");
        mockMvc.perform(put("/api/teams/" + galatasarayId).contentType(APPLICATION_JSON)
                        .content(teamBody("Cimbom", 1905)))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/teams/" + galatasarayId)).andExpect(status().isConflict());

        // Oluştururken oyuncu eklemek isteğe bağlı: eklenenler kadroya girer, eksik mevkiler 18'e tamamlanır
        String takim1Json = mockMvc.perform(post("/api/teams").contentType(APPLICATION_JSON).content(
                        "{\"name\": \"Takim 1\", \"foundedYear\": 1950, \"colors\": \"Mavi\", \"players\": ["
                                + playerBody("Kral Golcu", "FORWARD", 9, 100) + ", "
                                + playerBody("Usta Kaleci", "GOALKEEPER", 1, 70) + "]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int takim1CreatedId = JsonPath.read(takim1Json, "$.id");
        String takim1Squad = mockMvc.perform(get("/api/teams/" + takim1CreatedId + "/players"))
                .andExpect(jsonPath("$.length()").value(18))
                .andReturn().getResponse().getContentAsString();
        assertEquals(100, first(takim1Squad, "$[?(@.name == 'Kral Golcu')].strength"));
        List<?> goalkeepers = JsonPath.read(takim1Squad, "$[?(@.position == 'GOALKEEPER')]");
        assertEquals(2, goalkeepers.size(), "Eklenen kaleci + 1 rastgele kaleci");
        mockMvc.perform(post("/api/teams").contentType(APPLICATION_JSON).content(
                        "{\"name\": \"Cakisma FK\", \"foundedYear\": 1950, \"colors\": \"Mavi\", \"players\": ["
                                + playerBody("A", "FORWARD", 7, 50) + ", " + playerBody("B", "DEFENDER", 7, 50) + "]}"))
                .andExpect(status().isBadRequest());
        for (int i = 2; i <= 14; i++) {
            createTeam("Takim " + i).andExpect(status().isCreated());
        }

        // Kadro: 18 oyuncu, düzenlenebilir, forma numarası takımda tekil
        String squadJson = mockMvc.perform(get("/api/teams/" + galatasarayId + "/players"))
                .andExpect(jsonPath("$.length()").value(18))
                .andReturn().getResponse().getContentAsString();
        int firstPlayerId = JsonPath.read(squadJson, "$[0].id");
        int secondPlayerNumber = JsonPath.read(squadJson, "$[1].shirtNumber");
        mockMvc.perform(put("/api/players/" + firstPlayerId).contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "Yeni Isim", "position": "FORWARD", "shirtNumber": 1, "strength": 95, "age": 30}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Yeni Isim"))
                .andExpect(jsonPath("$.position").value("FORWARD"))
                .andExpect(jsonPath("$.strength").value(95));
        mockMvc.perform(put("/api/players/" + firstPlayerId).contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "Yeni Isim", "position": "FORWARD", "shirtNumber": %d, "strength": 95, "age": 30}
                                """.formatted(secondPlayerNumber)))
                .andExpect(status().isBadRequest());
        List<Integer> usedNumbers = JsonPath.read(squadJson, "$[*].shirtNumber");
        int freeNumber = IntStream.rangeClosed(2, 99)
                .filter(number -> !usedNumbers.contains(number)).findFirst().orElseThrow();
        mockMvc.perform(post("/api/teams/" + galatasarayId + "/players").contentType(APPLICATION_JSON)
                        .content(playerBody("Yeni Transfer", "MIDFIELDER", freeNumber, 80)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.goals").value(0));
        mockMvc.perform(get("/api/teams/" + galatasarayId + "/players")).andExpect(jsonPath("$.length()").value(19));

        // Fikstür: oynanmamış maçlarda olasılıklar toplamı 100
        String fixtureJson = mockMvc.perform(post("/api/fixtures/generate"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(34))
                .andExpect(jsonPath("$[*].matches.length()", everyItem(is(9))))
                .andReturn().getResponse().getContentAsString();
        List<Integer> homeWin = JsonPath.read(fixtureJson, "$[*].matches[*].homeWinProbability");
        List<Integer> draw = JsonPath.read(fixtureJson, "$[*].matches[*].drawProbability");
        List<Integer> awayWin = JsonPath.read(fixtureJson, "$[*].matches[*].awayWinProbability");
        for (int i = 0; i < homeWin.size(); i++) {
            assertEquals(100, homeWin.get(i) + draw.get(i) + awayWin.get(i));
        }

        createTeam("Gec Kalan FK").andExpect(status().isConflict());

        // Haftalar sırayla
        mockMvc.perform(post("/api/weeks/5/play"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", notNullValue()));
        String weekJson = mockMvc.perform(post("/api/weeks/1/play"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matches[*].played", everyItem(is(true))))
                .andReturn().getResponse().getContentAsString();
        mockMvc.perform(post("/api/weeks/1/play")).andExpect(status().isConflict());
        mockMvc.perform(post("/api/weeks/99/play")).andExpect(status().isNotFound());

        // Maç detayı: gol olayları skorla aynı, istatistikler tutarlı
        int matchId = JsonPath.read(weekJson, "$.matches[0].id");
        int homeScore = JsonPath.read(weekJson, "$.matches[0].homeScore");
        String detailJson = mockMvc.perform(get("/api/matches/" + matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.played").value(true))
                .andExpect(jsonPath("$.home.stats").exists())
                .andReturn().getResponse().getContentAsString();
        List<?> homeGoals = JsonPath.read(detailJson, "$.events[?(@.type == 'GOAL' && @.home == true)]");
        assertEquals(homeScore, homeGoals.size());
        int shots = JsonPath.read(detailJson, "$.home.stats.shots");
        int shotsOnTarget = JsonPath.read(detailJson, "$.home.stats.shotsOnTarget");
        assertTrue(shots >= shotsOnTarget && shotsOnTarget >= homeScore);
        mockMvc.perform(get("/api/matches/999999")).andExpect(status().isNotFound());

        // Sezonu tamamla
        String seasonJson = mockMvc.perform(post("/api/seasons/play-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finalStandings.length()").value(18))
                .andExpect(jsonPath("$.finalStandings[*].played", everyItem(is(34))))
                .andReturn().getResponse().getContentAsString();
        String championName = JsonPath.read(seasonJson, "$.championName");
        String seasonsJson = mockMvc.perform(get("/api/seasons"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].finished").value(true))
                .andExpect(jsonPath("$[0].championName").value(championName))
                .andExpect(jsonPath("$[0].playedMatches").value(306))
                .andReturn().getResponse().getContentAsString();
        int firstSeasonId = JsonPath.read(seasonsJson, "$[0].id");

        // Oyuncu istatistikleri: tüm goller oyunculara dağıtılmış ve saklanmış olmalı
        List<Integer> teamGoals = JsonPath.read(seasonJson, "$.finalStandings[*].goalsFor");
        String statsJson = mockMvc.perform(get("/api/players/stats"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Integer> playerGoals = JsonPath.read(statsJson, "$[*].goals");
        List<Integer> ownGoals = JsonPath.read(statsJson, "$[*].ownGoals");
        assertEquals(teamGoals.stream().mapToInt(Integer::intValue).sum(),
                playerGoals.stream().mapToInt(Integer::intValue).sum() + ownGoals.stream().mapToInt(Integer::intValue).sum(),
                "Takim golleri = oyuncu golleri + kendi kalesine goller");
        for (int i = 1; i < playerGoals.size(); i++) {
            assertTrue(playerGoals.get(i - 1) >= playerGoals.get(i), "Gol kralligi sirali olmali");
        }
        int galatasarayGoals = first(seasonJson, "$.finalStandings[?(@.teamName == 'Galatasaray')].goalsFor");
        List<Integer> galatasarayScorerGoals = JsonPath.read(statsJson,
                "$[?(@.teamId == " + galatasarayId + ")].goals");
        // Aradaki fark rakiplerin Galatasaray maçlarında kendi kalesine attığı goller
        int ownGoalsForGalatasaray = galatasarayGoals - galatasarayScorerGoals.stream().mapToInt(Integer::intValue).sum();
        assertTrue(ownGoalsForGalatasaray >= 0 && ownGoalsForGalatasaray <= 8, "Kendi kalesine: " + ownGoalsForGalatasaray);

        // Bölgeler ve küme düşme: 3 takım düştü (arşivde), yerine 3 takım çıktı; 4 büyükler düşmez
        assertEquals("CHAMPIONS_LEAGUE", JsonPath.read(seasonJson, "$.finalStandings[0].zone"));
        List<Integer> relegatedIds = JsonPath.read(seasonJson, "$.finalStandings[?(@.zone == 'RELEGATION')].teamId");
        assertEquals(3, relegatedIds.size());
        mockMvc.perform(get("/api/seasons"))
                .andExpect(jsonPath("$[0].relegated.length()").value(3))
                .andExpect(jsonPath("$[0].promoted.length()").value(3));
        String teamsAfterSeason = mockMvc.perform(get("/api/teams"))
                .andExpect(jsonPath("$.length()").value(18))
                .andReturn().getResponse().getContentAsString();
        List<Integer> activeIds = JsonPath.read(teamsAfterSeason, "$[*].id");
        assertTrue(relegatedIds.stream().noneMatch(activeIds::contains), "Dusen takimlar aktif listede olmamali");
        mockMvc.perform(get("/api/teams/" + relegatedIds.getFirst()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        List<Boolean> bigFourRelegated = JsonPath.read(teamsAfterSeason, "$[?(@.bigFour == true)].active");
        assertEquals(4, bigFourRelegated.size(), "4 buyuklerin hepsi ligde kalir");

        // Rekorlar, oyuncu profili, takım istatistikleri ve karşılaştırma
        mockMvc.perform(get("/api/records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title == 'En çok şampiyonluk')].holder").value(championName));
        int topScorerId = JsonPath.read(statsJson, "$[0].playerId");
        mockMvc.perform(get("/api/players/" + topScorerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seasons[0].goals").value(playerGoals.getFirst()))
                .andExpect(jsonPath("$.goals[?(@.type == 'GOAL')].length()").exists());
        mockMvc.perform(get("/api/teams/" + galatasarayId + "/stats").param("seasonId", String.valueOf(firstSeasonId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overall.played").value(34))
                .andExpect(jsonPath("$.home.played").value(17))
                .andExpect(jsonPath("$.overall.goalsFor").value(galatasarayGoals))
                .andExpect(jsonPath("$.weeks.length()").value(34))
                .andExpect(jsonPath("$.form.length()").value(5));
        int fenerbahceId = first(teamsAfterSeason, "$[?(@.name == 'Fenerbahçe')].id");
        mockMvc.perform(get("/api/teams/head-to-head").param("teamA", String.valueOf(galatasarayId))
                        .param("teamB", String.valueOf(fenerbahceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.played").value(2));

        // Kupa: lig bitince ilk 8; kupa yarıdayken yeni sezon açılamaz
        mockMvc.perform(get("/api/cup")).andExpect(jsonPath("$.status").value("NOT_STARTED"));
        mockMvc.perform(post("/api/cup/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.rounds[0].ties.length()").value(4))
                .andExpect(jsonPath("$.rounds[0].ties[0].homeSeed").value(1))
                .andExpect(jsonPath("$.rounds[0].ties[0].awaySeed").value(8));
        mockMvc.perform(post("/api/fixtures/generate")).andExpect(status().isConflict());
        String cupJson = mockMvc.perform(post("/api/cup/play-all"))
                .andExpect(jsonPath("$.status").value("FINISHED"))
                .andExpect(jsonPath("$.rounds.length()").value(3))
                .andExpect(jsonPath("$.rounds[2].ties.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        String cupWinner = JsonPath.read(cupJson, "$.winnerName");
        mockMvc.perform(get("/api/seasons")).andExpect(jsonPath("$[0].cupWinnerName").value(cupWinner));
        mockMvc.perform(get("/api/standings").param("seasonId", String.valueOf(firstSeasonId)))
                .andExpect(jsonPath("$[*].played", everyItem(is(34))));

        // Bitmiş sezon sıfırlanamaz; takım listesi yeniden değiştirilebilir
        mockMvc.perform(delete("/api/fixtures")).andExpect(status().isConflict());
        String newTeamJson = createTeam("Yeni Sezon FK").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int newTeamId = JsonPath.read(newTeamJson, "$.id");
        mockMvc.perform(delete("/api/teams/" + newTeamId)).andExpect(status().isNoContent());

        // Maç geçmişi olan takım silinince arşivlenir: aktif listeden çıkar, eski sezon tablosunda kalır
        List<Integer> midTableIds = JsonPath.read(seasonJson,
                "$.finalStandings[?(@.zone == null && @.teamName =~ /Takim.*/)].teamId");
        mockMvc.perform(delete("/api/teams/" + midTableIds.getFirst())).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/teams")).andExpect(jsonPath("$.length()").value(17));
        createTeam("Takim 99").andExpect(status().isCreated());

        // İkinci sezon
        mockMvc.perform(post("/api/fixtures/generate")).andExpect(status().isCreated());
        mockMvc.perform(get("/api/seasons"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].seasonNumber").value(2))
                .andExpect(jsonPath("$[0].finished").value(false));
        mockMvc.perform(get("/api/standings")).andExpect(jsonPath("$[*].played", everyItem(is(0))));
        mockMvc.perform(get("/api/standings").param("seasonId", String.valueOf(firstSeasonId)))
                .andExpect(jsonPath("$.length()").value(18))
                .andExpect(jsonPath("$[0].teamName").value(championName))
                .andExpect(jsonPath("$[*].played", everyItem(is(34))));
        mockMvc.perform(get("/api/teams")).andExpect(jsonPath("$[*].morale", everyItem(is(50))));

        // Yeni sezonda sezon istatistikleri sıfır; kariyer istatistikleri (lig + kupa) saklı ve en az lig golleri kadar
        String gsSquad = mockMvc.perform(get("/api/teams/" + galatasarayId + "/players"))
                .andReturn().getResponse().getContentAsString();
        List<Integer> seasonGoals = JsonPath.read(gsSquad, "$[*].goals");
        assertEquals(0, seasonGoals.stream().mapToInt(Integer::intValue).sum());
        List<Map<String, Object>> squad = JsonPath.read(gsSquad, "$[*]");
        for (Map<String, Object> player : squad) {
            List<Integer> leagueGoals = JsonPath.read(statsJson, "$[?(@.playerId == " + player.get("id") + ")].goals");
            int expectedAtLeast = leagueGoals.isEmpty() ? 0 : leagueGoals.getFirst();
            assertTrue((int) player.get("careerGoals") >= expectedAtLeast, "Kariyer golleri saklanmali: " + player);
        }
        mockMvc.perform(get("/api/players/stats").param("seasonId", String.valueOf(firstSeasonId)))
                .andExpect(jsonPath("$[0].goals").value(playerGoals.getFirst()));

        // Devam eden sezon sıfırlanır, arşivdeki sezon kalır
        mockMvc.perform(post("/api/weeks/1/play")).andExpect(status().isOk());
        mockMvc.perform(delete("/api/fixtures")).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/seasons")).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/teams")).andExpect(jsonPath("$[*].lastStrengthChange", everyItem(is(0))));
    }

    @Test
    void puanTablosundaSiraDegisimiIkinciHaftadanItibarenHesaplanir() throws Exception {
        mockMvc.perform(post("/api/teams/random").contentType(APPLICATION_JSON).content(randomBody(14)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(14));
        mockMvc.perform(get("/api/teams")).andExpect(jsonPath("$.length()").value(18));
        mockMvc.perform(post("/api/fixtures/generate")).andExpect(status().isCreated());
        mockMvc.perform(post("/api/teams/random").contentType(APPLICATION_JSON).content(randomBody(2)))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/weeks/1/play")).andExpect(status().isOk());
        mockMvc.perform(get("/api/standings")).andExpect(jsonPath("$[*].rankChange", everyItem(is(0))));

        mockMvc.perform(post("/api/weeks/2/play")).andExpect(status().isOk());
        String standings = mockMvc.perform(get("/api/standings")).andReturn().getResponse().getContentAsString();
        List<Integer> rankChanges = JsonPath.read(standings, "$[*].rankChange");
        assertEquals(0, rankChanges.stream().mapToInt(Integer::intValue).sum(),
                "Sira degisimlerinin toplami 0 olmali (biri yukselirken biri duser)");
    }

    @Test
    void gecersizIsteklerAlanBazliMesajlarla400Doner() throws Exception {
        mockMvc.perform(post("/api/teams")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "", "foundedYear": 1800, "colors": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("Takım adı boş olamaz"))
                .andExpect(jsonPath("$.foundedYear").value("Geçerli bir kuruluş yılı giriniz"))
                .andExpect(jsonPath("$.colors").value("Renkler boş olamaz"));

        mockMvc.perform(put("/api/players/1")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": " ", "position": "FORWARD", "shirtNumber": 120, "strength": 0, "age": 12}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.strength").value("Oyuncu gücü 1-100 arasında olmalı"))
                .andExpect(jsonPath("$.name").value("Oyuncu adı boş olamaz"))
                .andExpect(jsonPath("$.shirtNumber").value("Forma numarası 1-99 arasında olmalı"));

        mockMvc.perform(post("/api/teams/random").contentType(APPLICATION_JSON).content(randomBody(0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.count").value("Takım sayısı 1-50 arasında olmalı"));
    }

    private ResultActions createTeam(String name) throws Exception {
        return mockMvc.perform(post("/api/teams").contentType(APPLICATION_JSON).content(teamBody(name, 1950)));
    }

    /** Filtreli JsonPath ifadeleri her zaman liste döndürür; ilk elemanı alır. */
    private static int first(String json, String filterPath) {
        List<Integer> values = JsonPath.read(json, filterPath);
        return values.getFirst();
    }

    private static String playerBody(String name, String position, int shirtNumber, int strength) {
        return "{\"name\": \"%s\", \"position\": \"%s\", \"shirtNumber\": %d, \"strength\": %d, \"age\": 24}"
                .formatted(name, position, shirtNumber, strength);
    }

    private static String randomBody(int count) {
        return "{\"count\": " + count + "}";
    }

    private static String teamBody(String name, int foundedYear) {
        return """
                {"name": "%s", "foundedYear": %d, "colors": "Mavi-Beyaz"}
                """.formatted(name, foundedYear);
    }
}
