package com.footballleague;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EconomyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void butceDegisimiGelirGiderKayitlarininToplaminaEsitSampiyonVeKupaOdulAlir() throws Exception {
        Map<Integer, Long> initialBudgets = startSeason();
        String season = body(post("/api/seasons/play-all"));
        mockMvc.perform(post("/api/cup/play-all")).andExpect(status().isOk());

        String championName = JsonPath.read(season, "$.championName");
        String cup = body(get("/api/cup"));
        int cupWinnerId = JsonPath.read(cup, "$.winnerTeamId");
        for (Map.Entry<Integer, Long> initial : initialBudgets.entrySet()) {
            String finances = body(get("/api/teams/" + initial.getKey() + "/finances"));
            long budget = ((Number) JsonPath.read(finances, "$.budget")).longValue();
            long income = ((Number) JsonPath.read(finances, "$.income")).longValue();
            long expenses = ((Number) JsonPath.read(finances, "$.expenses")).longValue();
            assertEquals(initial.getValue() + income - expenses, budget, "Takım " + initial.getKey());
            assertTrue(expenses > 0, "Maaş ödendi");

            List<Number> leaguePrize = JsonPath.read(finances, "$.totals[?(@.type == 'LEAGUE_PRIZE')].amount");
            String name = JsonPath.read(body(get("/api/teams/" + initial.getKey())), "$.name");
            assertEquals(name.equals(championName) ? 20_000_000L : leaguePrize.getFirst().longValue(),
                    leaguePrize.getFirst().longValue());
            List<Number> cupPrize = JsonPath.read(finances, "$.totals[?(@.type == 'CUP_PRIZE')].amount");
            if (initial.getKey() == cupWinnerId) {
                assertEquals(6_000_000L, cupPrize.getFirst().longValue());
            }
        }
    }

    @Test
    void sezonSonundaSozlesmesiBitenlerUzatilirYaDaSerbestKalirKadrolarSablondaKalir() throws Exception {
        startSeason();
        mockMvc.perform(post("/api/seasons/play-all")).andExpect(status().isOk());

        String teams = body(get("/api/teams"));
        List<Integer> ids = JsonPath.read(teams, "$[*].id");
        for (Integer id : ids) {
            String squad = body(get("/api/teams/" + id + "/players"));
            List<Integer> contracts = JsonPath.read(squad, "$[*].contractUntil");
            assertTrue(contracts.stream().allMatch(until -> until != null && until >= 2),
                    "Sezon 1 sonunda biten sözleşmeler ya uzatıldı ya oyuncu ayrıldı: " + contracts);
            List<String> positions = JsonPath.read(squad, "$[*].position");
            assertTrue(positions.size() >= 18);
            assertTrue(positions.stream().filter("GOALKEEPER"::equals).count() >= 2);
            assertTrue(positions.stream().filter("FORWARD"::equals).count() >= 4);
            List<Number> wages = JsonPath.read(squad, "$[*].wage");
            assertTrue(wages.stream().allMatch(wage -> wage.longValue() > 0));
        }
    }

    @Test
    void sezonSifirlanincaButcelerSezonBasinaDoner() throws Exception {
        Map<Integer, Long> initialBudgets = startSeason();
        for (int week = 1; week <= 3; week++) {
            mockMvc.perform(post("/api/weeks/" + week + "/play")).andExpect(status().isOk());
        }
        mockMvc.perform(delete("/api/fixtures")).andExpect(status().isNoContent());
        assertEquals(initialBudgets, budgets());
    }

    private Map<Integer, Long> startSeason() throws Exception {
        mockMvc.perform(post("/api/teams/random").contentType(APPLICATION_JSON).content("{\"count\": 14}"))
                .andExpect(status().isCreated());
        Map<Integer, Long> budgets = budgets();
        mockMvc.perform(post("/api/fixtures/generate")).andExpect(status().isCreated());
        return budgets;
    }

    private Map<Integer, Long> budgets() throws Exception {
        String teams = body(get("/api/teams"));
        List<Integer> ids = JsonPath.read(teams, "$[*].id");
        List<Number> budgets = JsonPath.read(teams, "$[*].budget");
        Map<Integer, Long> result = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            result.put(ids.get(i), budgets.get(i).longValue());
        }
        return result;
    }

    private String body(org.springframework.test.web.servlet.RequestBuilder request) throws Exception {
        return mockMvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }
}
