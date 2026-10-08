package com.footballleague;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

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
class TacticsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void herTakiminHocasiVarDizilisDegisirMactaUygulanir() throws Exception {
        mockMvc.perform(post("/api/teams/random").contentType(APPLICATION_JSON).content("{\"count\": 14}"))
                .andExpect(status().isCreated());
        String teams = mockMvc.perform(get("/api/teams"))
                .andExpect(jsonPath("$[*].manager.name", everyItem(notNullValue())))
                .andExpect(jsonPath("$[*].formation", everyItem(notNullValue())))
                .andReturn().getResponse().getContentAsString();
        int teamId = JsonPath.read(teams, "$[0].id");

        mockMvc.perform(put("/api/teams/" + teamId + "/tactics").contentType(APPLICATION_JSON)
                        .content("{\"formation\": \"F352\", \"playStyle\": \"DEFENSIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formation").value("F352"))
                .andExpect(jsonPath("$.playStyle").value("DEFENSIVE"));
        mockMvc.perform(put("/api/teams/" + teamId + "/tactics").contentType(APPLICATION_JSON)
                        .content("{\"formation\": null, \"playStyle\": \"DEFENSIVE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.formation").exists());

        String fixture = mockMvc.perform(post("/api/fixtures/generate")).andReturn().getResponse().getContentAsString();
        mockMvc.perform(post("/api/weeks/1/play")).andExpect(status().isOk());
        List<Integer> matchIds = JsonPath.read(fixture, "$[0].matches[?(@.homeTeamId == " + teamId
                + " || @.awayTeamId == " + teamId + ")].id");
        String detail = mockMvc.perform(get("/api/matches/" + matchIds.getFirst()))
                .andReturn().getResponse().getContentAsString();
        boolean home = ((Integer) JsonPath.read(detail, "$.home.teamId")) == teamId;
        String side = home ? "$.home" : "$.away";
        assertEquals("F352", JsonPath.read(detail, side + ".stats.formation"));
        List<String> starters = JsonPath.read(detail, side + ".lineup[?(@.starter == true)].position");
        assertEquals(3, starters.stream().filter("DEFENDER"::equals).count());
        assertEquals(5, starters.stream().filter("MIDFIELDER"::equals).count());
    }
}
