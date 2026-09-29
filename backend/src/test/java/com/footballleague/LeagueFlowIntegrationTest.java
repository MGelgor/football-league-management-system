package com.footballleague;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LeagueFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void takimEkleFiksturOlusturHaftaOynatSezonuTamamlaSifirla() throws Exception {
        for (int i = 1; i <= 18; i++) {
            createTeam("Takim " + i).andExpect(status().isCreated());
        }

        mockMvc.perform(post("/api/fixtures/generate"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(34))
                .andExpect(jsonPath("$[*].matches.length()", everyItem(is(9))));

        createTeam("Gec Kalan FK")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", notNullValue()));

        mockMvc.perform(post("/api/weeks/1/play"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matches[*].played", everyItem(is(true))));
        mockMvc.perform(post("/api/weeks/1/play")).andExpect(status().isConflict());
        mockMvc.perform(post("/api/weeks/99/play")).andExpect(status().isNotFound());

        String seasonJson = mockMvc.perform(post("/api/season/play-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finalStandings.length()").value(18))
                .andExpect(jsonPath("$.finalStandings[*].played", everyItem(is(34))))
                .andReturn().getResponse().getContentAsString();
        String championName = JsonPath.read(seasonJson, "$.championName");
        int championPoints = JsonPath.read(seasonJson, "$.championPoints");

        mockMvc.perform(get("/api/standings"))
                .andExpect(jsonPath("$[0].teamName").value(championName))
                .andExpect(jsonPath("$[0].points").value(championPoints));
        mockMvc.perform(get("/api/fixtures"))
                .andExpect(jsonPath("$[*].matches[*].played", everyItem(is(true))));

        mockMvc.perform(delete("/api/fixtures")).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/fixtures")).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/standings")).andExpect(jsonPath("$[*].played", everyItem(is(0))));
        mockMvc.perform(get("/api/teams")).andExpect(jsonPath("$[*].morale", everyItem(is(50))));
        createTeam("Yeni Sezon FK").andExpect(status().isCreated());
    }

    @Test
    void gecersizTakimIstegiAlanBazliMesajlarla400Doner() throws Exception {
        mockMvc.perform(post("/api/teams")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "", "foundedYear": 1800, "colors": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("Takım adı boş olamaz"))
                .andExpect(jsonPath("$.foundedYear").value("Geçerli bir kuruluş yılı giriniz"))
                .andExpect(jsonPath("$.colors").value("Renkler boş olamaz"));
    }

    private ResultActions createTeam(String name) throws Exception {
        return mockMvc.perform(post("/api/teams")
                .contentType(APPLICATION_JSON)
                .content("""
                        {"name": "%s", "foundedYear": 1950, "colors": "Mavi-Beyaz"}
                        """.formatted(name)));
    }
}
