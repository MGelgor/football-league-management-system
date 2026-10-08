package com.footballleague;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MyTeamIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void haftaKullaniciMacindaBeklerSecilenKadroOynarCanliMactaDevreArasiDegisiklikYapilir() throws Exception {
        mockMvc.perform(get("/api/my-team")).andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(post("/api/teams/random").contentType(APPLICATION_JSON).content("{\"count\": 14}"))
                .andExpect(status().isCreated());
        int teamId = JsonPath.<List<Integer>>read(body(get("/api/teams")), "$[?(@.name == 'Galatasaray')].id").getFirst();
        mockMvc.perform(post("/api/my-team").contentType(APPLICATION_JSON)
                        .content("{\"managerName\": \"Hoca\", \"teamId\": " + teamId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.team.id").value(teamId))
                .andExpect(jsonPath("$.nextMatch").doesNotExist());
        mockMvc.perform(post("/api/fixtures/generate")).andExpect(status().isCreated());

        // Kadro seçilmeden hafta oynamaz
        mockMvc.perform(get("/api/my-team")).andExpect(jsonPath("$.nextMatch.weekNumber").value(1))
                .andExpect(jsonPath("$.nextMatch.lineupSaved").value(false));
        mockMvc.perform(post("/api/weeks/1/play")).andExpect(status().isConflict());

        // Önerilen kadro: 11 oyuncu; geçersiz kadrolar 400
        String lineup = body(get("/api/my-team/lineup"));
        List<Integer> suggested = JsonPath.read(lineup, "$.starterIds");
        assertEquals(11, suggested.size());
        mockMvc.perform(put("/api/my-team/lineup").contentType(APPLICATION_JSON)
                        .content(lineupBody("F442", suggested.subList(0, 10), suggested.get(0), suggested.get(5))))
                .andExpect(status().isBadRequest());
        // Önerilen 11, başka bir dizilişin mevki sayılarına uymaz
        String otherFormation = "F352".equals(JsonPath.read(lineup, "$.formation")) ? "F442" : "F352";
        mockMvc.perform(put("/api/my-team/lineup").contentType(APPLICATION_JSON)
                        .content(lineupBody(otherFormation, suggested, suggested.get(0), suggested.get(5))))
                .andExpect(status().isBadRequest());

        // Geçerli kadro: 4-4-2'de yedek forvetlerden biri ilk 11'de
        List<Map<String, Object>> squad = JsonPath.read(lineup, "$.squad");
        List<Integer> starters = chooseEleven(squad);
        Integer forward = starters.getLast();
        mockMvc.perform(put("/api/my-team/lineup").contentType(APPLICATION_JSON)
                        .content(lineupBody("F442", starters, starters.get(1), forward)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(true));
        String week = body(post("/api/weeks/1/play"));
        int matchId = JsonPath.<List<Integer>>read(week,
                "$.matches[?(@.homeTeamId == " + teamId + " || @.awayTeamId == " + teamId + ")].id").getFirst();
        String detail = body(get("/api/matches/" + matchId));
        String side = ((Integer) JsonPath.read(detail, "$.home.teamId")) == teamId ? "$.home" : "$.away";
        List<Integer> playedStarters = JsonPath.read(detail, side + ".lineup[?(@.starter == true)].playerId");
        assertEquals(new HashSet<>(starters), new HashSet<>(playedStarters), "Seçilen ilk 11 oynadı");

        // Canlı maç: ilk yarı → devre arası değişiklik → ikinci yarı
        mockMvc.perform(post("/api/my-team/live/second-half").contentType(APPLICATION_JSON)
                        .content("{\"playStyle\": \"BALANCED\", \"talk\": \"NONE\"}"))
                .andExpect(status().isConflict());
        String half = body(post("/api/my-team/live/start"));
        assertEquals(45, (int) JsonPath.read(half, "$.minute"));
        List<Integer> firstHalfMinutes = JsonPath.read(half, "$.events[*].minute");
        assertTrue(firstHalfMinutes.stream().allMatch(minute -> minute <= 45));
        assertEquals(half, body(post("/api/my-team/live/start")), "Aynı oturum döner");
        List<Map<String, Object>> onPitch = JsonPath.read(half, "$.onPitch");
        List<Map<String, Object>> bench = JsonPath.read(half, "$.bench");
        Map<String, Object> outgoing = onPitch.stream().filter(p -> !"GOALKEEPER".equals(p.get("position")))
                .reduce((a, b) -> b).orElseThrow();
        Map<String, Object> incoming = bench.getFirst();
        String full = body(post("/api/my-team/live/second-half").contentType(APPLICATION_JSON)
                .content("{\"substitutions\": [{\"outId\": " + outgoing.get("id") + ", \"inId\": " + incoming.get("id")
                        + "}], \"playStyle\": \"ATTACKING\", \"talk\": \"MOTIVATE\"}"));
        assertEquals(true, JsonPath.read(full, "$.finished"));
        int liveMatchId = JsonPath.read(full, "$.matchId");
        String liveDetail = body(get("/api/matches/" + liveMatchId));
        assertEquals((int) JsonPath.read(full, "$.homeScore"), (int) JsonPath.read(liveDetail, "$.home.score"));
        assertEquals((int) JsonPath.read(full, "$.awayScore"), (int) JsonPath.read(liveDetail, "$.away.score"));
        List<Integer> subMinute = JsonPath.read(liveDetail, "$..lineup[?(@.playerId == " + incoming.get("id") + ")].minuteOn");
        assertEquals(46, subMinute.getFirst());
        String liveSide = ((Integer) JsonPath.read(liveDetail, "$.home.teamId")) == teamId ? "$.home" : "$.away";
        assertEquals("ATTACKING", JsonPath.read(liveDetail, liveSide + ".stats.playStyle"));
        mockMvc.perform(get("/api/fixtures")).andExpect(jsonPath("$[1].matches[*].played", everyItem(is(true))));
        assertEquals(8, ((List<?>) JsonPath.read(full, "$.otherResults")).size());

        // Hızlı oynat: kadro seçmeden (yapay zekâ) hafta 3 oynanır
        mockMvc.perform(post("/api/my-team/play")).andExpect(status().isOk())
                .andExpect(jsonPath("$.nextMatch.weekNumber").value(4))
                .andExpect(jsonPath("$.recentResults", hasSize(3)));

        // Mod kapanınca takıma yapay zekâ hocası gelir, haftalar beklemeden oynar
        mockMvc.perform(delete("/api/my-team")).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/my-team")).andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(get("/api/teams/" + teamId)).andExpect(jsonPath("$.manager.name").exists());
        mockMvc.perform(post("/api/weeks/4/play")).andExpect(status().isOk());
    }

    @Test
    void tumSezonuOynatYonetilenTakimdaOtomatikIzinIsterKupaMaciCanliOynanir() throws Exception {
        mockMvc.perform(post("/api/teams/random").contentType(APPLICATION_JSON).content("{\"count\": 14}"))
                .andExpect(status().isCreated());
        List<Integer> bigFour = JsonPath.read(body(get("/api/teams")), "$[?(@.bigFour == true)].id");
        int teamId = bigFour.getFirst();
        mockMvc.perform(post("/api/my-team").contentType(APPLICATION_JSON)
                .content("{\"managerName\": \"Hoca\", \"teamId\": " + teamId + "}")).andExpect(status().isOk());
        mockMvc.perform(post("/api/fixtures/generate")).andExpect(status().isCreated());
        mockMvc.perform(post("/api/seasons/play-all")).andExpect(status().isConflict());
        mockMvc.perform(post("/api/seasons/play-all?auto=true")).andExpect(status().isOk());
        mockMvc.perform(post("/api/cup/start")).andExpect(status().isOk());

        String dashboard = body(get("/api/my-team"));
        if (JsonPath.read(dashboard, "$.nextMatch") == null) {
            return; // Takım ilk 8'e giremediyse kupada maçı yok
        }
        assertEquals("CUP", JsonPath.read(dashboard, "$.nextMatch.competition"));
        mockMvc.perform(post("/api/cup/play-round")).andExpect(status().isConflict());
        body(post("/api/my-team/live/start"));
        String full = body(post("/api/my-team/live/second-half").contentType(APPLICATION_JSON)
                .content("{\"playStyle\": \"BALANCED\", \"talk\": \"CALM\"}"));
        assertEquals(3, ((List<?>) JsonPath.read(full, "$.otherResults")).size());
        mockMvc.perform(get("/api/cup"))
                .andExpect(jsonPath("$.rounds[0].played").value(true))
                .andExpect(jsonPath("$.rounds[1].ties", hasSize(2)));
    }

    /** Kadrodan 1 KL, 4 DEF, 4 OS, 2 FV: forvetlerde en zayıf uygun forvet sona konur. */
    private static List<Integer> chooseEleven(List<Map<String, Object>> squad) {
        List<Integer> chosen = new ArrayList<>();
        Map<String, Integer> need = Map.of("GOALKEEPER", 1, "DEFENDER", 4, "MIDFIELDER", 4, "FORWARD", 2);
        for (String position : List.of("GOALKEEPER", "DEFENDER", "MIDFIELDER", "FORWARD")) {
            List<Map<String, Object>> candidates = squad.stream()
                    .filter(p -> position.equals(p.get("position")) && (Boolean) p.get("available"))
                    .sorted((a, b) -> (Integer) b.get("strength") - (Integer) a.get("strength"))
                    .toList();
            if (position.equals("FORWARD")) {
                chosen.add((Integer) candidates.getFirst().get("id"));
                chosen.add((Integer) candidates.getLast().get("id"));
            } else {
                candidates.stream().limit(need.get(position)).forEach(p -> chosen.add((Integer) p.get("id")));
            }
        }
        return chosen;
    }

    private static String lineupBody(String formation, List<Integer> starters, int captain, int penaltyTaker) {
        return "{\"formation\": \"" + formation + "\", \"playStyle\": \"BALANCED\", \"starterIds\": " + starters
                + ", \"captainId\": " + captain + ", \"penaltyTakerId\": " + penaltyTaker + "}";
    }

    private String body(RequestBuilder request) throws Exception {
        return mockMvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }
}
