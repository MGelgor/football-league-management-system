package com.footballleague;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
class RefereeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void herMacaHakemAtanirAyniHaftadaHakemTekrarlanmazIstatistiklerToplanir() throws Exception {
        mockMvc.perform(get("/api/referees")).andExpect(jsonPath("$.length()").value(14))
                .andExpect(jsonPath("$[0].matches").value(0));
        mockMvc.perform(post("/api/teams/random").contentType(APPLICATION_JSON).content("{\"count\": 14}"))
                .andExpect(status().isCreated());
        String fixture = mockMvc.perform(post("/api/fixtures/generate")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        mockMvc.perform(post("/api/seasons/play-all")).andExpect(status().isOk());

        List<Integer> weekOneIds = JsonPath.read(fixture, "$[0].matches[*].id");
        Set<String> referees = new HashSet<>();
        for (Integer id : weekOneIds) {
            String detail = mockMvc.perform(get("/api/matches/" + id)).andReturn().getResponse().getContentAsString();
            referees.add(JsonPath.read(detail, "$.referee.name"));
        }
        assertEquals(weekOneIds.size(), referees.size(), "Aynı haftada her maça farklı hakem");

        String list = mockMvc.perform(get("/api/referees")).andReturn().getResponse().getContentAsString();
        List<Integer> matches = JsonPath.read(list, "$[*].matches");
        assertEquals(306, matches.stream().mapToInt(Integer::intValue).sum());
        List<Integer> yellow = JsonPath.read(list, "$[*].yellowCards");
        assertTrue(yellow.stream().mapToInt(Integer::intValue).sum() > 306, "Maç başına birden fazla sarı kart");
        List<Integer> strictness = JsonPath.read(list, "$[*].strictness");
        for (int i = 1; i < strictness.size(); i++) {
            assertTrue(strictness.get(i - 1) >= strictness.get(i), "En sert hakem başta");
        }
    }
}
