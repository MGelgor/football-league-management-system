package com.footballleague;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LiveBroadcastIntegrationTest {

    private static final Pattern MINUTE_ID = Pattern.compile("event:minute\\nid:(\\d+)");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void oynanmisHaftaDakikaSirasiylaYayinlanirSkorlarGercekSonucaUlasir() throws Exception {
        startSeason();
        mockMvc.perform(get("/api/weeks/1/live?speed=0")).andExpect(status().isConflict());
        String week = mockMvc.perform(post("/api/weeks/1/play")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String stream = stream(get("/api/weeks/1/live?speed=0"));
        assertTrue(stream.startsWith("event:start\n"), stream.substring(0, 40));
        assertEquals(range(0, 90), minuteIds(stream));
        assertTrue(stream.trim().endsWith("}]") && stream.contains("event:end\n"));

        // 90. dakikadaki skorlar maç sonuçlarıyla aynı
        String lastMinute = stream.substring(stream.lastIndexOf("event:minute\nid:90\ndata:") + 24);
        lastMinute = lastMinute.substring(0, lastMinute.indexOf('\n'));
        List<Integer> homeScores = JsonPath.read(week, "$.matches[*].homeScore");
        List<Integer> streamedHome = JsonPath.read(lastMinute, "$.scores[*].home");
        assertEquals(homeScores, streamedHome);
        assertEquals("FULL_TIME", JsonPath.read(lastMinute, "$.phase"));

        // Gollerin yorum cümlesi var
        if (stream.contains("\"type\":\"GOAL\"")) {
            assertTrue(stream.contains("GOL!"));
        }
    }

    @Test
    void yenidenBaglananIstemciKaldigiDakikadanDevamEder() throws Exception {
        startSeason();
        mockMvc.perform(post("/api/weeks/1/play")).andExpect(status().isOk());

        assertEquals(range(41, 90), minuteIds(stream(get("/api/weeks/1/live?speed=0").header("Last-Event-ID", "40"))));
        assertEquals(range(75, 90), minuteIds(stream(get("/api/weeks/1/live?speed=0&from=75"))));
        mockMvc.perform(get("/api/weeks/1/live?speed=7")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/weeks/99/live")).andExpect(status().isNotFound());
    }

    @Test
    void kupaTuruAyniYayinlaIzlenirVeMacDetayindaYorumVar() throws Exception {
        startSeason();
        mockMvc.perform(post("/api/seasons/play-all")).andExpect(status().isOk());
        mockMvc.perform(post("/api/cup/start")).andExpect(status().isOk());
        String cup = mockMvc.perform(post("/api/cup/play-round"))
                .andExpect(jsonPath("$.rounds[0].weekNumber").value(101))
                .andReturn().getResponse().getContentAsString();
        assertEquals(range(0, 90), minuteIds(stream(get("/api/weeks/101/live?speed=0"))));

        int matchId = JsonPath.read(cup, "$.rounds[0].ties[0].match.id");
        String detail = mockMvc.perform(get("/api/matches/" + matchId)).andReturn().getResponse().getContentAsString();
        List<String> commentary = JsonPath.read(detail, "$.events[*].commentary");
        assertTrue(commentary.stream().allMatch(text -> text != null && !text.isBlank()));
    }

    private void startSeason() throws Exception {
        mockMvc.perform(post("/api/teams/random").contentType(APPLICATION_JSON).content("{\"count\": 14}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/fixtures/generate")).andExpect(status().isCreated());
    }

    private String stream(MockHttpServletRequestBuilder request) throws Exception {
        MvcResult result = mockMvc.perform(request).andExpect(request().asyncStarted()).andReturn();
        mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk());
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static List<Integer> minuteIds(String stream) {
        List<Integer> ids = new ArrayList<>();
        Matcher matcher = MINUTE_ID.matcher(stream);
        while (matcher.find()) {
            ids.add(Integer.parseInt(matcher.group(1)));
        }
        return ids;
    }

    private static List<Integer> range(int from, int to) {
        List<Integer> list = new ArrayList<>();
        for (int i = from; i <= to; i++) {
            list.add(i);
        }
        return list;
    }
}
