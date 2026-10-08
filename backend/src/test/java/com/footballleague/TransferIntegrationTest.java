package com.footballleague;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class TransferIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void pencereSezonBoyuncaKapaliSezonBitinceAcikTekliflerKuralaGoreSonuclanir() throws Exception {
        mockMvc.perform(post("/api/teams/random").contentType(APPLICATION_JSON).content("{\"count\": 14}"))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/transfers/window")).andExpect(jsonPath("$.open").value(false));
        mockMvc.perform(post("/api/fixtures/generate")).andExpect(status().isCreated());
        mockMvc.perform(post("/api/transfers/offers").contentType(APPLICATION_JSON)
                        .content("{\"playerId\": 1, \"buyerTeamId\": 2, \"fee\": 1000}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/seasons/play-all")).andExpect(status().isOk());
        mockMvc.perform(get("/api/transfers/window"))
                .andExpect(jsonPath("$.open").value(true))
                .andExpect(jsonPath("$.upcomingSeasonNumber").value(2));

        String market = body(get("/api/transfers/market"));
        List<Map<String, Object>> freeAgents = JsonPath.read(market, "$[?(@.freeAgent == true)]");
        assertTrue(freeAgents.size() >= 12, "Serbest oyuncu havuzu: " + freeAgents.size());

        // Satılabilir bir oyuncu: kadrosu 18'den büyük ve o mevkide şablon fazlası olan takımdan
        Map<String, Object> target = sellablePlayer(market);
        int playerId = (Integer) target.get("playerId");
        int sellerId = (Integer) target.get("teamId");
        long asking = ((Number) target.get("askingPrice")).longValue();
        int buyerId = richestOtherTeam(sellerId, asking);
        long buyerBudget = budget(buyerId);
        long sellerBudget = budget(sellerId);

        offer(playerId, buyerId, asking / 2).andExpect(jsonPath("$.status").value("REJECTED"));
        offer(playerId, buyerId, Math.round(asking * 0.9)).andExpect(jsonPath("$.status").value("COUNTER"))
                .andExpect(jsonPath("$.askingPrice").value(asking));
        offer(playerId, buyerId, asking).andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.transfer.toTeamId").value(buyerId))
                .andExpect(jsonPath("$.transfer.fee").value(asking));

        assertEquals(buyerBudget - asking, budget(buyerId));
        assertEquals(sellerBudget + asking, budget(sellerId));
        String buyerSquad = body(get("/api/teams/" + buyerId + "/players"));
        List<Integer> numbers = JsonPath.read(buyerSquad, "$[?(@.id == " + playerId + ")].shirtNumber");
        assertTrue(numbers.getFirst() > 0, "Yeni forma numarası");
        mockMvc.perform(get("/api/players/" + playerId))
                .andExpect(jsonPath("$.teamId").value(buyerId))
                .andExpect(jsonPath("$.transfers[-1].fromTeamId").value(sellerId));
        offer(playerId, buyerId, asking).andExpect(status().isBadRequest());

        // Bütçe yetmezse 400
        int poorestId = poorestTeam();
        Map<String, Object> expensive = expensivePlayerNotOf(market, poorestId, budget(poorestId));
        offer((Integer) expensive.get("playerId"), poorestId, budget(poorestId) + 10_000_000)
                .andExpect(status().isBadRequest());

        // Serbest oyuncu: bedelsiz, maaşla
        int freeAgentId = (Integer) freeAgents.getFirst().get("playerId");
        mockMvc.perform(post("/api/transfers/free-agents/" + freeAgentId + "/sign").contentType(APPLICATION_JSON)
                        .content("{\"teamId\": " + buyerId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fromTeamId").doesNotExist())
                .andExpect(jsonPath("$.fee").value(0));
        mockMvc.perform(get("/api/transfers")).andExpect(jsonPath("$[0].playerId").value(freeAgentId));
    }

    @Test
    void yeniSezonBaslayincaPencereKapanirKadrolarSablondaDortBuyuklerGucBandinda() throws Exception {
        mockMvc.perform(post("/api/teams/random").contentType(APPLICATION_JSON).content("{\"count\": 14}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/fixtures/generate")).andExpect(status().isCreated());
        mockMvc.perform(post("/api/seasons/play-all")).andExpect(status().isOk());
        mockMvc.perform(post("/api/fixtures/generate")).andExpect(status().isCreated());
        mockMvc.perform(get("/api/transfers/window")).andExpect(jsonPath("$.open").value(false));

        String teams = body(get("/api/teams"));
        List<Integer> ids = JsonPath.read(teams, "$[*].id");
        for (Integer id : ids) {
            String squad = body(get("/api/teams/" + id + "/players"));
            List<String> positions = JsonPath.read(squad, "$[*].position");
            assertTrue(positions.size() >= 18 && positions.size() <= 30, "Kadro " + positions.size());
            assertTrue(positions.stream().filter("GOALKEEPER"::equals).count() >= 2);
            assertTrue(positions.stream().filter("DEFENDER"::equals).count() >= 6);
            assertTrue(positions.stream().filter("MIDFIELDER"::equals).count() >= 6);
            assertTrue(positions.stream().filter("FORWARD"::equals).count() >= 4);
            List<Integer> numbers = JsonPath.read(squad, "$[*].shirtNumber");
            assertEquals(numbers.size(), numbers.stream().distinct().count(), "Forma numaraları tekil");
        }
        mockMvc.perform(get("/api/teams"))
                .andExpect(jsonPath("$[?(@.bigFour == true)].strength", everyItem(greaterThanOrEqualTo(85))))
                .andExpect(jsonPath("$[*].budget", everyItem(greaterThanOrEqualTo(0))));
        List<?> transfers = JsonPath.read(body(get("/api/transfers")), "$");
        assertTrue(!transfers.isEmpty(), "Yapay zekâ transfer yaptı");
    }

    private Map<String, Object> sellablePlayer(String market) throws Exception {
        String teams = body(get("/api/teams"));
        for (Integer teamId : JsonPath.<List<Integer>>read(teams, "$[?(@.bigFour == false)].id")) {
            String squad = body(get("/api/teams/" + teamId + "/players"));
            List<String> positions = JsonPath.read(squad, "$[*].position");
            if (positions.size() <= 18) {
                continue;
            }
            for (String position : List.of("DEFENDER", "MIDFIELDER", "FORWARD", "GOALKEEPER")) {
                long count = positions.stream().filter(position::equals).count();
                long template = switch (position) {
                    case "GOALKEEPER" -> 2;
                    case "FORWARD" -> 4;
                    default -> 6;
                };
                if (count > template) {
                    List<Map<String, Object>> candidates = JsonPath.read(market,
                            "$[?(@.teamId == " + teamId + " && @.position == '" + position + "')]");
                    return candidates.getLast();
                }
            }
        }
        throw new AssertionError("Satılabilir oyuncu yok");
    }

    private int richestOtherTeam(int excludedId, long atLeast) throws Exception {
        String teams = body(get("/api/teams"));
        List<Map<String, Object>> list = JsonPath.read(teams, "$[?(@.id != " + excludedId + ")]");
        Map<String, Object> richest = list.stream()
                .max((a, b) -> Long.compare(((Number) a.get("budget")).longValue(), ((Number) b.get("budget")).longValue()))
                .orElseThrow();
        assertTrue(((Number) richest.get("budget")).longValue() >= atLeast);
        return (Integer) richest.get("id");
    }

    private int poorestTeam() throws Exception {
        List<Map<String, Object>> list = JsonPath.read(body(get("/api/teams")), "$[*]");
        return (Integer) list.stream()
                .min((a, b) -> Long.compare(((Number) a.get("budget")).longValue(), ((Number) b.get("budget")).longValue()))
                .orElseThrow().get("id");
    }

    private Map<String, Object> expensivePlayerNotOf(String market, int teamId, long budget) {
        List<Map<String, Object>> list = JsonPath.read(market, "$[?(@.freeAgent == false && @.teamId != " + teamId + ")]");
        return list.getFirst();
    }

    private org.springframework.test.web.servlet.ResultActions offer(int playerId, int buyerId, long fee)
            throws Exception {
        return mockMvc.perform(post("/api/transfers/offers").contentType(APPLICATION_JSON)
                .content("{\"playerId\": " + playerId + ", \"buyerTeamId\": " + buyerId + ", \"fee\": " + fee + "}"));
    }

    private long budget(int teamId) throws Exception {
        return ((Number) JsonPath.read(body(get("/api/teams/" + teamId)), "$.budget")).longValue();
    }

    private String body(RequestBuilder request) throws Exception {
        return mockMvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }
}
