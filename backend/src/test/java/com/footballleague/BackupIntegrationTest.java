package com.footballleague;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BackupIntegrationTest {

    @TempDir
    static Path saveDir;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.save-dir", () -> saveDir.toString());
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void disaAktarilanYedekGeriYuklenincePuanDurumuBireBirAyniOlur() throws Exception {
        startSeasonAndPlayWeeks(3);
        String standingsBefore = body(get("/api/standings"));
        String backup = mockMvc.perform(get("/api/data/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("futbol-ligi-yedek-")))
                .andExpect(jsonPath("$.summary.teamCount").value(18))
                .andExpect(jsonPath("$.summary.playedMatches").value(27))
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(post("/api/weeks/4/play")).andExpect(status().isOk());
        assertNotEquals(standingsBefore, body(get("/api/standings")));

        mockMvc.perform(multipart("/api/data/import")
                        .file(new MockMultipartFile("file", "yedek.json", "application/json",
                                backup.getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isNoContent());
        assertEquals(standingsBefore, body(get("/api/standings")));

        // id sayaçları yedekteki en büyük id'nin üstünden devam eder: yeni maç kayıtları çakışmaz
        mockMvc.perform(post("/api/weeks/5/play")).andExpect(status().isConflict());
        mockMvc.perform(post("/api/weeks/4/play")).andExpect(status().isOk());
        mockMvc.perform(post("/api/seasons/play-all")).andExpect(status().isOk());
    }

    @Test
    void kayitNoktasiKaydedilirYuklenirSilinir() throws Exception {
        startSeasonAndPlayWeeks(2);
        String standingsBefore = body(get("/api/standings"));

        mockMvc.perform(post("/api/data/saves").contentType(APPLICATION_JSON).content("{\"name\": \"Hafta 2 öncesi\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.summary.seasonNumber").value(1))
                .andExpect(jsonPath("$.summary.playedMatches").value(18));
        mockMvc.perform(get("/api/data/saves"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Hafta 2 öncesi"));

        mockMvc.perform(post("/api/seasons/play-all")).andExpect(status().isOk());
        mockMvc.perform(post("/api/data/saves/Hafta 2 öncesi/load")).andExpect(status().isNoContent());
        assertEquals(standingsBefore, body(get("/api/standings")));

        mockMvc.perform(delete("/api/data/saves/Hafta 2 öncesi")).andExpect(status().isNoContent());
        mockMvc.perform(post("/api/data/saves/Hafta 2 öncesi/load")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/data/saves")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void gecersizYedekVeKayitAdiReddedilir() throws Exception {
        mockMvc.perform(multipart("/api/data/import")
                        .file(new MockMultipartFile("file", "x.json", "application/json", "merhaba".getBytes())))
                .andExpect(status().isBadRequest());
        mockMvc.perform(multipart("/api/data/import")
                        .file(new MockMultipartFile("file", "x.json", "application/json",
                                "{\"formatVersion\": 99, \"tables\": {}}".getBytes())))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/data/saves").contentType(APPLICATION_JSON).content("{\"name\": \"../disari\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists());
        // Hata sonrası veri bozulmadı
        mockMvc.perform(get("/api/teams")).andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    void csvDosyalariNoktaliVirgulVeBomIleUretilir() throws Exception {
        startSeasonAndPlayWeeks(1);
        String standings = mockMvc.perform(get("/api/data/csv/standings"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(standings.startsWith("﻿Sıra;Takım;O;G;B;M;A;Y;AV;P\r\n"));
        assertEquals(19, standings.split("\r\n").length);

        String fixture = mockMvc.perform(get("/api/data/csv/fixture"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertEquals(1 + 306, fixture.split("\r\n").length);

        String players = mockMvc.perform(get("/api/data/csv/players"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(players.contains("Forvet") || players.contains("Orta saha"));
    }

    private void startSeasonAndPlayWeeks(int weeks) throws Exception {
        mockMvc.perform(post("/api/teams/random").contentType(APPLICATION_JSON).content("{\"count\": 14}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/fixtures/generate")).andExpect(status().isCreated());
        for (int week = 1; week <= weeks; week++) {
            mockMvc.perform(post("/api/weeks/" + week + "/play")).andExpect(status().isOk());
        }
    }

    private String body(org.springframework.test.web.servlet.RequestBuilder request) throws Exception {
        return mockMvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }
}
