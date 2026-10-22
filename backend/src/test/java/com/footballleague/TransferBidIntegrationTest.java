package com.footballleague;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.footballleague.service.RoomScheduler;
import com.jayway.jsonpath.JsonPath;

/** Odada transfer penceresi: kapalı teklifler pencere kapanınca çözülür, en yüksek teklif kazanır. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TransferBidIntegrationTest {

    @TestConfiguration
    static class ClockConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(Instant.parse("2026-03-02T10:00:00Z"));
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MutableClock clock;

    @Autowired
    private RoomScheduler roomScheduler;

    private record Session(String code, String token) {
    }

    @Test
    void enYuksekKapaliTeklifKazanirInsaninOyuncusunaTeklifGelenKutusunaGider() throws Exception {
        Session owner = createRoom();
        Session ayse = join(owner.code(), "Ayşe");
        Session bora = join(owner.code(), "Bora");
        List<Integer> teamIds = JsonPath.read(body(in(get("/api/teams"), owner)), "$[*].id");
        int ayseTeam = teamIds.get(4);
        takeTeam(ayse, "Ayşe", ayseTeam);
        mockMvc.perform(post("/api/rooms/" + owner.code() + "/start").header("X-Room-Token", owner.token()))
                .andExpect(status().isOk());

        // 34 hafta + 3 kupa turu oynanır, transfer penceresi açılır
        clock.set(Instant.parse("2026-04-07T19:00:00Z"));
        assertEquals(37, roomScheduler.tickAll());

        // Bora pencerede, sezonu bot yönetmiş (sözleşmeleri uzatılmış, satılabilir oyuncusu olan) bir takımı alır
        long ayseBudget = budget(ayse, ayseTeam);
        List<Map<String, Object>> market = JsonPath.read(body(in(get("/api/transfers/market"), ayse)), "$[*]");
        Map<String, Object> boraPlayer = market.stream()
                .filter(player -> !(Boolean) player.get("freeAgent"))
                .filter(player -> ((Number) player.get("teamId")).intValue() != ayseTeam)
                .filter(player -> ((Number) player.get("askingPrice")).longValue() <= ayseBudget / 2)
                .findFirst().orElseThrow();
        int boraTeam = ((Number) boraPlayer.get("teamId")).intValue();
        takeTeam(bora, "Bora", boraTeam);

        long budget = Math.min(ayseBudget, budget(bora, boraTeam));
        Map<String, Object> target = market.stream()
                .filter(player -> !(Boolean) player.get("freeAgent"))
                .filter(player -> !List.of(ayseTeam, boraTeam).contains(((Number) player.get("teamId")).intValue()))
                .filter(player -> ((Number) player.get("askingPrice")).longValue() + 100_000 <= budget)
                .max(Comparator.comparingLong(player -> ((Number) player.get("askingPrice")).longValue()))
                .orElseThrow();
        int targetId = (Integer) target.get("playerId");
        long asking = ((Number) target.get("askingPrice")).longValue();
        List<Integer> freeAgents = JsonPath.read(body(in(get("/api/transfers/market"), ayse)),
                "$[?(@.freeAgent == true)].playerId");

        offer(ayse, targetId, ayseTeam, asking).andExpect(jsonPath("$.status").value("BID"));
        offer(bora, targetId, boraTeam, asking + 50_000).andExpect(jsonPath("$.status").value("BID"));
        offer(ayse, targetId, ayseTeam, asking / 2).andExpect(jsonPath("$.status").value("REJECTED"));
        offer(ayse, targetId, boraTeam, asking).andExpect(status().isBadRequest());
        offer(owner, targetId, ayseTeam, asking).andExpect(status().isBadRequest());
        // Serbest oyuncu: eşit teklifte önce veren kazanır
        offer(ayse, freeAgents.get(0), ayseTeam, 0).andExpect(jsonPath("$.status").value("BID"));
        offer(bora, freeAgents.get(0), boraTeam, 0).andExpect(jsonPath("$.status").value("BID"));
        offer(ayse, freeAgents.get(1), ayseTeam, 0).andExpect(jsonPath("$.status").value("BID"));
        Integer withdrawn = JsonPath.<List<Integer>>read(body(in(get("/api/transfers/bids"), ayse)),
                "$[?(@.playerId == " + freeAgents.get(1) + ")].id").getFirst();
        mockMvc.perform(in(delete("/api/transfers/bids/" + withdrawn), bora)).andExpect(status().isBadRequest());
        mockMvc.perform(in(delete("/api/transfers/bids/" + withdrawn), ayse)).andExpect(status().isNoContent());
        mockMvc.perform(in(get("/api/transfers/bids"), ayse)).andExpect(jsonPath("$.length()").value(2));

        // İnsanın oyuncusuna teklif: Bora'nın gelen kutusuna düşer, kabul edince hemen geçer
        int boraPlayerId = (Integer) boraPlayer.get("playerId");
        offer(ayse, boraPlayerId, ayseTeam, ((Number) boraPlayer.get("askingPrice")).longValue())
                .andExpect(jsonPath("$.status").value("SENT"));
        Integer offerId = JsonPath.<List<Integer>>read(body(in(get("/api/my-team/inbox"), bora)),
                "$[?(@.type == 'TRANSFER_OFFER' && @.actionable == true && @.playerId == " + boraPlayerId + ")].id")
                .getFirst();
        mockMvc.perform(in(post("/api/my-team/inbox/" + offerId + "/accept"), bora)).andExpect(status().isOk());
        mockMvc.perform(in(get("/api/teams/" + ayseTeam + "/players"), owner))
                .andExpect(jsonPath("$[*].id", hasItem(boraPlayerId)));

        // Herkes hazır: pencere kapanır, teklifler çözülür
        for (Session session : List.of(ayse, bora, owner)) {
            mockMvc.perform(post("/api/rooms/" + owner.code() + "/ready").header("X-Room-Token", session.token())
                    .contentType(APPLICATION_JSON).content("{\"ready\":true}")).andExpect(status().isOk());
        }
        mockMvc.perform(in(get("/api/transfers/bids"), ayse))
                .andExpect(jsonPath("$[?(@.playerId == " + targetId + ")].status").value("LOST"))
                .andExpect(jsonPath("$[?(@.playerId == " + freeAgents.get(0) + ")].status").value("WON"));
        mockMvc.perform(in(get("/api/transfers/bids"), bora))
                .andExpect(jsonPath("$[?(@.playerId == " + targetId + ")].status").value("WON"))
                .andExpect(jsonPath("$[?(@.playerId == " + freeAgents.get(0) + ")].status").value("LOST"));
        mockMvc.perform(in(get("/api/teams/" + boraTeam + "/players"), owner))
                .andExpect(jsonPath("$[*].id", hasItem(targetId)));
        List<String> titles = JsonPath.read(body(in(get("/api/my-team/inbox"), ayse)), "$[*].title");
        assertTrue(titles.stream().anyMatch(title -> title.startsWith("Teklifin kazandı")), titles.toString());
    }

    private long budget(Session session, int teamId) throws Exception {
        return ((Number) JsonPath.read(body(in(get("/api/teams/" + teamId), session)), "$.budget")).longValue();
    }

    private ResultActions offer(Session session, int playerId, int teamId, long fee) throws Exception {
        return mockMvc.perform(in(post("/api/transfers/offers"), session).contentType(APPLICATION_JSON)
                .content("{\"playerId\":" + playerId + ",\"buyerTeamId\":" + teamId + ",\"fee\":" + fee + "}"));
    }

    private void takeTeam(Session session, String name, int teamId) throws Exception {
        mockMvc.perform(in(post("/api/my-team"), session).contentType(APPLICATION_JSON)
                .content("{\"managerName\":\"" + name + "\",\"teamId\":" + teamId + "}")).andExpect(status().isOk());
    }

    private Session createRoom() throws Exception {
        String body = mockMvc.perform(post("/api/rooms").contentType(APPLICATION_JSON)
                .content("{\"roomName\":\"Oda\",\"nickname\":\"Ali\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return new Session(JsonPath.read(body, "$.room.code"), JsonPath.read(body, "$.token"));
    }

    private Session join(String code, String nickname) throws Exception {
        String body = mockMvc.perform(post("/api/rooms/" + code + "/join").contentType(APPLICATION_JSON)
                .content("{\"nickname\":\"" + nickname + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return new Session(code, JsonPath.read(body, "$.token"));
    }

    private String body(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private static MockHttpServletRequestBuilder in(MockHttpServletRequestBuilder request, Session session) {
        return request.header("X-Room-Code", session.code()).header("X-Room-Token", session.token());
    }
}
