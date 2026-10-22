package com.footballleague;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.footballleague.service.RoomScheduler;
import com.jayway.jsonpath.JsonPath;

/** Bir odada birden çok insan menajer: takım seçimi, yetkiler, ayrı gelen kutuları, kadro kilidi, bot devralma. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RoomManagersIntegrationTest {

    // 2 Mart 2026, 13:00 İstanbul; maç saati 21:00 = 18:00 UTC
    private static final Instant START = Instant.parse("2026-03-02T10:00:00Z");

    @TestConfiguration
    static class ClockConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(START);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MutableClock clock;

    @Autowired
    private RoomScheduler roomScheduler;

    private record Session(String code, String token, Long memberId) {
    }

    @BeforeEach
    void resetClock() {
        clock.set(START);
    }

    @Test
    void herUyeKendiTakiminiYonetirKadroMacaBesDakikaKalaKilitlenir() throws Exception {
        Session owner = createRoom();
        Session ayse = join(owner.code(), "Ayşe");
        Session bora = join(owner.code(), "Bora");
        List<Integer> teamIds = JsonPath.read(body(in(get("/api/teams"), owner)), "$[*].id");
        int first = teamIds.get(5);
        int second = teamIds.get(6);
        String secondName = JsonPath.read(body(in(get("/api/teams/" + second), owner)), "$.name");

        takeTeam(ayse, "Ayşe", first).andExpect(status().isOk()).andExpect(jsonPath("$.team.id").value(first));
        takeTeam(bora, "Bora", first).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Ayşe")));
        takeTeam(bora, "Bora", second).andExpect(status().isOk());
        mockMvc.perform(in(get("/api/my-team"), ayse)).andExpect(jsonPath("$.team.id").value(first));
        mockMvc.perform(in(get("/api/my-team"), bora)).andExpect(jsonPath("$.team.id").value(second));
        mockMvc.perform(in(get("/api/my-team"), owner)).andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(get("/api/rooms/" + owner.code()).header("X-Room-Token", owner.token()))
                .andExpect(jsonPath("$.members[?(@.nickname == 'Bora')].teamName").value(secondName));

        // Taktik yalnızca kendi takımına; takımı olmayan oda sahibi de başkasınınkini değiştiremez
        String tactics = "{\"formation\":\"F433\",\"playStyle\":\"ATTACKING\"}";
        mockMvc.perform(in(put("/api/teams/" + first + "/tactics"), bora).contentType(APPLICATION_JSON).content(tactics))
                .andExpect(status().isForbidden());
        mockMvc.perform(in(put("/api/teams/" + second + "/tactics"), bora).contentType(APPLICATION_JSON).content(tactics))
                .andExpect(status().isOk());
        mockMvc.perform(in(put("/api/teams/" + second + "/tactics"), owner).contentType(APPLICATION_JSON).content(tactics))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/rooms/" + owner.code() + "/start").header("X-Room-Token", owner.token()))
                .andExpect(jsonPath("$.nextEventAt").value("2026-03-02T18:00:00Z"));
        // Gelen kutuları ayrı: Ayşe, Bora'nın hoş geldin mesajını görmez
        List<String> ayseTitles = JsonPath.read(body(in(get("/api/my-team/inbox"), ayse)), "$[*].title");
        assertFalse(ayseTitles.isEmpty());
        assertTrue(ayseTitles.stream().noneMatch(title -> title.contains(secondName)), ayseTitles.toString());

        String lineup = body(in(get("/api/my-team/lineup"), ayse));
        mockMvc.perform(in(put("/api/my-team/lineup"), ayse).contentType(APPLICATION_JSON).content(lineupBody(lineup)))
                .andExpect(status().isOk());
        clock.set(Instant.parse("2026-03-02T17:56:00Z"));
        mockMvc.perform(in(put("/api/my-team/lineup"), ayse).contentType(APPLICATION_JSON).content(lineupBody(lineup)))
                .andExpect(status().isConflict());
        mockMvc.perform(in(put("/api/teams/" + second + "/tactics"), bora).contentType(APPLICATION_JSON).content(tactics))
                .andExpect(status().isConflict());

        // Maç günü oynandı: kilit ertesi güne geçer, sıradaki maçın kadrosu verilebilir
        clock.set(Instant.parse("2026-03-02T18:01:00Z"));
        String nextLineup = body(in(get("/api/my-team/lineup"), ayse));
        mockMvc.perform(in(put("/api/my-team/lineup"), ayse).contentType(APPLICATION_JSON).content(lineupBody(nextLineup)))
                .andExpect(status().isOk());
        mockMvc.perform(in(get("/api/standings"), ayse)).andExpect(jsonPath("$[0].played").value(1));
    }

    @Test
    void ayrilanVeAtilanUyeninTakiminiBotDevralir() throws Exception {
        Session owner = createRoom();
        Session ayse = join(owner.code(), "Ayşe");
        Session bora = join(owner.code(), "Bora");
        List<Integer> teamIds = JsonPath.read(body(in(get("/api/teams"), owner)), "$[*].id");
        takeTeam(ayse, "Ayşe", teamIds.get(5)).andExpect(status().isOk());
        takeTeam(bora, "Bora", teamIds.get(6)).andExpect(status().isOk());
        mockMvc.perform(in(get("/api/teams/" + teamIds.get(6)), owner)).andExpect(jsonPath("$.manager").isEmpty());

        mockMvc.perform(delete("/api/rooms/" + owner.code() + "/members/me").header("X-Room-Token", bora.token()))
                .andExpect(status().isNoContent());
        mockMvc.perform(in(get("/api/teams/" + teamIds.get(6)), owner)).andExpect(jsonPath("$.manager.name").exists());

        mockMvc.perform(delete("/api/rooms/" + owner.code() + "/members/" + owner.memberId())
                .header("X-Room-Token", ayse.token())).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/rooms/" + owner.code() + "/members/" + owner.memberId())
                .header("X-Room-Token", owner.token())).andExpect(status().isConflict());
        mockMvc.perform(delete("/api/rooms/" + owner.code() + "/members/" + ayse.memberId())
                .header("X-Room-Token", owner.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members", hasSize(1)));
        mockMvc.perform(in(get("/api/teams"), ayse)).andExpect(status().isForbidden());
        mockMvc.perform(in(get("/api/teams/" + teamIds.get(5)), owner)).andExpect(jsonPath("$.manager.name").exists());
        // Takım boşaldı: yeni gelen biri devralabilir
        Session can = join(owner.code(), "Can");
        takeTeam(can, "Can", teamIds.get(5)).andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions takeTeam(Session session, String name, int teamId)
            throws Exception {
        return mockMvc.perform(in(post("/api/my-team"), session).contentType(APPLICATION_JSON)
                .content("{\"managerName\":\"" + name + "\",\"teamId\":" + teamId + "}"));
    }

    private static String lineupBody(String lineup) {
        List<Integer> starters = JsonPath.read(lineup, "$.starterIds");
        return "{\"formation\":\"" + JsonPath.read(lineup, "$.formation") + "\",\"playStyle\":\"BALANCED\","
                + "\"starterIds\":" + starters + ",\"captainId\":" + starters.get(0) + ",\"penaltyTakerId\":"
                + starters.get(starters.size() - 1) + "}";
    }

    private Session createRoom() throws Exception {
        String body = mockMvc.perform(post("/api/rooms").contentType(APPLICATION_JSON)
                .content("{\"roomName\":\"Oda\",\"nickname\":\"Ali\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return session(body);
    }

    private Session join(String code, String nickname) throws Exception {
        return session(mockMvc.perform(post("/api/rooms/" + code + "/join").contentType(APPLICATION_JSON)
                .content("{\"nickname\":\"" + nickname + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private static Session session(String body) {
        return new Session(JsonPath.read(body, "$.room.code"), JsonPath.read(body, "$.token"),
                ((Number) JsonPath.read(body, "$.room.you")).longValue());
    }

    private String body(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private static MockHttpServletRequestBuilder in(MockHttpServletRequestBuilder request, Session session) {
        return request.header("X-Room-Code", session.code()).header("X-Room-Token", session.token());
    }
}
