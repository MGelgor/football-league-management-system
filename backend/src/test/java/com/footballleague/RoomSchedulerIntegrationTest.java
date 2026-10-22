package com.footballleague;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;

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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.footballleague.service.RoomScheduler;
import com.jayway.jsonpath.JsonPath;

/** Oda takvimi: saat elle ilerletilir. Maç saati 21:00 İstanbul = 18:00 UTC. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RoomSchedulerIntegrationTest {

    // 2 Mart 2026, 13:00 İstanbul
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

    private record Session(String code, String token) {
    }

    @BeforeEach
    void resetClock() {
        clock.set(START);
    }

    @Test
    void takvimHerGunBirHaftaOynatirKacanGunleriYetistirir() throws Exception {
        Session owner = createRoom();
        room(post("/api/rooms/" + owner.code() + "/start"), owner)
                .andExpect(jsonPath("$.status").value("SEASON"))
                .andExpect(jsonPath("$.nextEventAt").value("2026-03-02T18:00:00Z"))
                .andExpect(jsonPath("$.nextMatchDay").value("1. hafta"))
                .andExpect(jsonPath("$.seasonNumber").value(1));

        clock.set(Instant.parse("2026-03-02T17:59:00Z"));
        assertEquals(0, roomScheduler.tickAll());
        clock.set(Instant.parse("2026-03-02T18:00:00Z"));
        assertEquals(1, roomScheduler.tickAll());
        roomInfo(owner).andExpect(jsonPath("$.nextMatchDay").value("2. hafta"))
                .andExpect(jsonPath("$.nextEventAt").value("2026-03-03T18:00:00Z"));

        // Sunucu 3 gün uyudu: kaçırılan 3 maç günü sırayla oynanır, tekrar tetiklemek bir şey değiştirmez
        clock.set(Instant.parse("2026-03-05T18:30:00Z"));
        assertEquals(3, roomScheduler.tickAll());
        assertEquals(0, roomScheduler.tickAll());
        roomInfo(owner).andExpect(jsonPath("$.nextMatchDay").value("5. hafta"))
                .andExpect(jsonPath("$.nextEventAt").value("2026-03-06T18:00:00Z"));

        // Odaya gelen herhangi bir istek de zamanı gelen günü oynatır; sonuçlar canlı yayın bitince açılır
        clock.set(Instant.parse("2026-03-06T18:01:00Z"));
        mockMvc.perform(in(get("/api/teams"), owner)).andExpect(status().isOk());
        mockMvc.perform(in(get("/api/standings"), owner)).andExpect(status().isConflict());
        clock.set(Instant.parse("2026-03-06T18:10:00Z"));
        mockMvc.perform(in(get("/api/standings"), owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].played").value(5));

        mockMvc.perform(in(post("/api/weeks/6/play"), owner)).andExpect(status().isForbidden());
        mockMvc.perform(in(post("/api/fixtures/generate"), owner)).andExpect(status().isForbidden());
    }

    @Test
    void sezonSonundaKupaVeTransferPenceresiHerkesHazirsaErkenKapanir() throws Exception {
        Session owner = createRoom();
        Session member = join(owner.code());
        room(post("/api/rooms/" + owner.code() + "/start"), owner).andExpect(status().isOk());

        // 34 lig haftası (2 Mart – 4 Nisan) + 3 kupa turu (5–7 Nisan); final 7 Nisan 21:00'de
        clock.set(Instant.parse("2026-04-07T19:00:00Z"));
        assertEquals(37, roomScheduler.tickAll());
        roomInfo(owner).andExpect(jsonPath("$.status").value("TRANSFER_WINDOW"))
                .andExpect(jsonPath("$.nextEventAt").value("2026-04-08T18:00:00Z"));
        mockMvc.perform(in(get("/api/seasons"), owner))
                .andExpect(jsonPath("$[0].finished").value(true))
                .andExpect(jsonPath("$[0].cupWinnerName").isNotEmpty());

        ready(member, true).andExpect(jsonPath("$.status").value("TRANSFER_WINDOW"));
        ready(owner, true)
                .andExpect(jsonPath("$.status").value("SEASON"))
                .andExpect(jsonPath("$.seasonNumber").value(2))
                .andExpect(jsonPath("$.nextMatchDay").value("1. hafta"))
                .andExpect(jsonPath("$.nextEventAt").value("2026-04-08T18:00:00Z"))
                .andExpect(jsonPath("$.members[0].ready").value(false));
    }

    @Test
    void lobideHerkesHazirOluncaLigBaslarAyarlarVeSimdiOynatYalnizcaSahip() throws Exception {
        Session owner = createRoom();
        Session member = join(owner.code());
        String settings = "{\"matchTime\":\"20:30\",\"timeZone\":\"Europe/Istanbul\",\"transferHours\":12}";

        mockMvc.perform(put("/api/rooms/" + owner.code() + "/settings").header("X-Room-Token", member.token())
                .contentType(APPLICATION_JSON).content(settings)).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/rooms/" + owner.code() + "/settings").header("X-Room-Token", owner.token())
                .contentType(APPLICATION_JSON)
                .content("{\"matchTime\":\"20:30\",\"timeZone\":\"Mars/Olympus\",\"transferHours\":12}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/rooms/" + owner.code() + "/settings").header("X-Room-Token", owner.token())
                .contentType(APPLICATION_JSON).content(settings))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchTime").value("20:30"))
                .andExpect(jsonPath("$.transferHours").value(12));

        ready(owner, true).andExpect(jsonPath("$.status").value("LOBBY"));
        ready(member, true)
                .andExpect(jsonPath("$.status").value("SEASON"))
                .andExpect(jsonPath("$.nextEventAt").value("2026-03-02T17:30:00Z"));

        mockMvc.perform(post("/api/rooms/" + owner.code() + "/run-now").header("X-Room-Token", member.token()))
                .andExpect(status().isForbidden());
        // Bugünün maçı erken oynanır; takvim kaymaz, sıradaki gün yarın
        room(post("/api/rooms/" + owner.code() + "/run-now"), owner)
                .andExpect(jsonPath("$.nextMatchDay").value("2. hafta"))
                .andExpect(jsonPath("$.nextEventAt").value("2026-03-03T17:30:00Z"));

        clock.advance(Duration.ofMinutes(1));
        mockMvc.perform(get("/internal/tick")).andExpect(status().isOk()).andExpect(jsonPath("$.events").value(0));
        mockMvc.perform(get("/internal/tick")).andExpect(jsonPath("$.skipped").value(true));
    }

    private Session createRoom() throws Exception {
        String body = mockMvc.perform(post("/api/rooms").contentType(APPLICATION_JSON)
                .content("{\"roomName\":\"Oda\",\"nickname\":\"Ali\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new Session(JsonPath.read(body, "$.room.code"), JsonPath.read(body, "$.token"));
    }

    private Session join(String code) throws Exception {
        String body = mockMvc.perform(post("/api/rooms/" + code + "/join").contentType(APPLICATION_JSON)
                .content("{\"nickname\":\"Veli\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new Session(code, JsonPath.read(body, "$.token"));
    }

    private ResultActions room(MockHttpServletRequestBuilder request, Session session) throws Exception {
        return mockMvc.perform(request.header("X-Room-Token", session.token())).andExpect(status().isOk());
    }

    private ResultActions roomInfo(Session session) throws Exception {
        return room(get("/api/rooms/" + session.code()), session);
    }

    private ResultActions ready(Session session, boolean ready) throws Exception {
        return room(post("/api/rooms/" + session.code() + "/ready").contentType(APPLICATION_JSON)
                .content("{\"ready\":" + ready + "}"), session);
    }

    private static MockHttpServletRequestBuilder in(MockHttpServletRequestBuilder request, Session session) {
        return request.header("X-Room-Code", session.code()).header("X-Room-Token", session.token());
    }
}
