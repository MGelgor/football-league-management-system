package com.footballleague;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.hamcrest.Matchers;
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

/** Maç günü saatinde oynanır, 10 dakika canlı yayınlanır; bu sürede sonuçlar API'den sızmaz. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RoomLiveIntegrationTest {

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

    @Test
    void macGunuCanliYayinlanirkenSonuclarGizlenir() throws Exception {
        String body = mockMvc.perform(post("/api/rooms").contentType(APPLICATION_JSON)
                .content("{\"roomName\":\"Oda\",\"nickname\":\"Ali\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String code = JsonPath.read(body, "$.room.code");
        String token = JsonPath.read(body, "$.token");
        mockMvc.perform(post("/api/rooms/" + code + "/start").header("X-Room-Token", token))
                .andExpect(jsonPath("$.liveWeek").isEmpty());

        // 21:00 İstanbul: 1. hafta oynanır, 21:10'a kadar canlı
        clock.set(Instant.parse("2026-03-02T18:00:00Z"));
        roomScheduler.tickAll();
        clock.set(Instant.parse("2026-03-02T18:04:00Z"));
        mockMvc.perform(get("/api/rooms/" + code).header("X-Room-Token", token))
                .andExpect(jsonPath("$.liveWeek").value(1))
                .andExpect(jsonPath("$.liveTitle").value("1. hafta"))
                .andExpect(jsonPath("$.liveUntil").value("2026-03-02T18:10:00Z"));
        for (String path : new String[] {"/api/standings", "/api/fixtures", "/api/matches/1", "/api/my-team",
                "/api/players/stats", "/api/seasons", "/api/teams/1"}) {
            mockMvc.perform(in(get(path), code, token)).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", Matchers.containsString("canlı")));
        }
        mockMvc.perform(in(get("/api/teams"), code, token)).andExpect(status().isOk());
        mockMvc.perform(in(get("/api/weeks/1/live"), code, token)).andExpect(request().asyncStarted());
        // Tek kişilik lig etkilenmez
        mockMvc.perform(get("/api/standings")).andExpect(status().isOk());

        clock.set(Instant.parse("2026-03-02T18:10:00Z"));
        mockMvc.perform(in(get("/api/standings"), code, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].played").value(1));
        mockMvc.perform(get("/api/rooms/" + code).header("X-Room-Token", token))
                .andExpect(jsonPath("$.liveWeek").isEmpty());

        // Sahip ertesi günü erken oynatırsa yayın o an başlar
        clock.set(Instant.parse("2026-03-02T19:00:00Z"));
        mockMvc.perform(post("/api/rooms/" + code + "/run-now").header("X-Room-Token", token))
                .andExpect(jsonPath("$.liveWeek").value(2))
                .andExpect(jsonPath("$.liveUntil").value("2026-03-02T19:10:00Z"));
        mockMvc.perform(in(get("/api/standings"), code, token)).andExpect(status().isConflict());
    }

    private static MockHttpServletRequestBuilder in(MockHttpServletRequestBuilder request, String code,
            String token) {
        return request.header("X-Room-Code", code).header("X-Room-Token", token);
    }
}
