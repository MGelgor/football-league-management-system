package com.footballleague;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Comparator;
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

import com.footballleague.repository.ManagerProfileRepository;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CareerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ManagerProfileRepository managerProfileRepository;

    @Test
    void hedefTutmayanMenajerKovulurIsTeklifiKabulEdilinceYeniTakimdaKariyerSurer() throws Exception {
        createTeams();
        List<Map<String, Object>> teams = JsonPath.read(body(get("/api/teams")), "$[*]");
        Map<String, Object> weakest = teams.stream()
                .min(Comparator.comparingInt(team -> (Integer) team.get("strength"))).orElseThrow();
        int teamId = (Integer) weakest.get("id");
        startManaging(teamId);
        mockMvc.perform(post("/api/fixtures/generate")).andExpect(status().isCreated());
        String dashboard = body(get("/api/my-team"));
        assertEquals(60, (int) JsonPath.read(dashboard, "$.confidence"));
        assertTrue((Integer) JsonPath.read(dashboard, "$.targetRank") >= 1);
        List<String> types = JsonPath.read(body(get("/api/my-team/inbox")), "$[*].type");
        assertTrue(types.contains("WELCOME") && types.contains("BOARD"), types.toString());

        // Yönetim kurulu şampiyonluk istesin, güven bitmek üzere: sezon sonunda kovulur
        managerProfileRepository.findAll().forEach(profile -> {
            profile.setConfidence(1);
            profile.setTargetRank(1);
            managerProfileRepository.save(profile);
        });
        mockMvc.perform(post("/api/seasons/play-all?auto=true")).andExpect(status().isOk());

        String afterSeason = body(get("/api/my-team"));
        assertEquals(false, JsonPath.read(afterSeason, "$.active"));
        assertEquals(true, JsonPath.read(afterSeason, "$.unemployed"));
        mockMvc.perform(get("/api/teams/" + teamId)).andExpect(jsonPath("$.manager.name").exists());
        String inbox = body(get("/api/my-team/inbox"));
        List<Integer> offers = JsonPath.read(inbox, "$[?(@.type == 'JOB_OFFER' && @.actionable == true)].id");
        assertFalse(offers.isEmpty(), "İş teklifi geldi");
        assertFalse(((List<?>) JsonPath.read(inbox, "$[?(@.type == 'SACKED')]")).isEmpty());

        mockMvc.perform(post("/api/my-team/inbox/" + offers.getFirst() + "/accept")).andExpect(status().isOk());
        String newDashboard = body(get("/api/my-team"));
        assertEquals(true, JsonPath.read(newDashboard, "$.active"));
        int newTeamId = JsonPath.read(newDashboard, "$.team.id");
        assertTrue(newTeamId != teamId);
        mockMvc.perform(post("/api/my-team/inbox/" + offers.getLast() + "/accept")).andExpect(status().isBadRequest());

        String career = body(get("/api/my-team/career"));
        assertEquals(2, ((List<?>) JsonPath.read(career, "$.spells")).size());
        assertTrue(List.of("Güven kalmadı", "Küme düştü").contains(JsonPath.read(career, "$.spells[0].endReason")));
        assertEquals(34, (int) JsonPath.read(career, "$.spells[0].matches"));
        assertEquals(34, (int) JsonPath.read(career, "$.matches"));
    }

    @Test
    void sozlesmePazarligiAltyapiTerfisiVeSatisaCikarilanOyuncununTeklifiyleSatis() throws Exception {
        createTeams();
        int teamId = JsonPath.<List<Integer>>read(body(get("/api/teams")), "$[?(@.name == 'Galatasaray')].id").getFirst();
        startManaging(teamId);
        mockMvc.perform(post("/api/fixtures/generate")).andExpect(status().isCreated());

        // Sözleşme: çok düşük teklif ret, isteğin %90'ı karşı istek, istek kadar kabul
        String squad = body(get("/api/teams/" + teamId + "/players"));
        int playerId = JsonPath.read(squad, "$[5].id");
        String rejected = body(post("/api/my-team/contracts/" + playerId).contentType(APPLICATION_JSON)
                .content("{\"weeklyWage\": 100, \"seasons\": 3}"));
        assertEquals("REJECTED", JsonPath.read(rejected, "$.status"));
        long demand = ((Number) JsonPath.read(rejected, "$.amount")).longValue();
        assertEquals("COUNTER", JsonPath.read(body(post("/api/my-team/contracts/" + playerId)
                .contentType(APPLICATION_JSON).content("{\"weeklyWage\": " + demand * 9 / 10 + ", \"seasons\": 3}")),
                "$.status"));
        assertEquals("ACCEPTED", JsonPath.read(body(post("/api/my-team/contracts/" + playerId)
                .contentType(APPLICATION_JSON).content("{\"weeklyWage\": " + demand + ", \"seasons\": 3}")), "$.status"));
        String updated = body(get("/api/teams/" + teamId + "/players"));
        assertEquals(3, (int) JsonPath.<List<Integer>>read(updated, "$[?(@.id == " + playerId + ")].contractUntil").getFirst());
        assertEquals(demand, JsonPath.<List<Number>>read(updated, "$[?(@.id == " + playerId + ")].wage").getFirst().longValue());

        // Sezon sonu: altyapıdan gençler menajerin kararını bekler
        mockMvc.perform(post("/api/seasons/play-all?auto=true")).andExpect(status().isOk());
        List<Map<String, Object>> academy = JsonPath.read(body(get("/api/my-team/academy")), "$[*]");
        assertFalse(academy.isEmpty());
        String inbox = body(get("/api/my-team/inbox"));
        List<Integer> youth = JsonPath.read(inbox, "$[?(@.type == 'YOUTH' && @.actionable == true)].id");
        assertEquals(academy.size(), youth.size());
        mockMvc.perform(post("/api/my-team/inbox/" + youth.getFirst() + "/accept")).andExpect(status().isOk());
        if (youth.size() > 1) {
            mockMvc.perform(post("/api/my-team/inbox/" + youth.getLast() + "/reject")).andExpect(status().isOk());
        }
        List<?> remaining = JsonPath.read(body(get("/api/my-team/academy")), "$[*]");
        assertEquals(Math.max(0, academy.size() - 2), remaining.size());

        // Terfi eden gencin mevkisinde bir oyuncu fazlası var: en zayıfı satışa çıkar, gelen teklifi kabul et
        String newSquad = body(get("/api/teams/" + teamId + "/players"));
        int promotedId = JsonPath.<List<Integer>>read(inbox, "$[?(@.id == " + youth.getFirst() + ")].playerId").getFirst();
        String position = JsonPath.<List<String>>read(newSquad, "$[?(@.id == " + promotedId + ")].position").getFirst();
        List<Map<String, Object>> samePosition = JsonPath.read(newSquad, "$[?(@.position == '" + position + "')]");
        Map<String, Object> toSell = samePosition.stream()
                .filter(p -> !p.get("id").equals(promotedId))
                .min(Comparator.comparingInt(p -> (Integer) p.get("strength"))).orElseThrow();
        long budgetBefore = ((Number) JsonPath.read(body(get("/api/teams/" + teamId)), "$.budget")).longValue();
        String sale = body(post("/api/my-team/sell/" + toSell.get("id")));
        if (((Number) JsonPath.read(sale, "$.amount")).intValue() == 0) {
            return; // Alıcı çıkmadı (bütçeler yetmedi)
        }
        String offerInbox = body(get("/api/my-team/inbox"));
        List<Map<String, Object>> transferOffers = JsonPath.read(offerInbox,
                "$[?(@.type == 'TRANSFER_OFFER' && @.actionable == true && @.playerId == " + toSell.get("id") + ")]");
        Map<String, Object> offer = transferOffers.getFirst();
        mockMvc.perform(post("/api/my-team/inbox/" + offer.get("id") + "/accept")).andExpect(status().isOk());
        mockMvc.perform(get("/api/players/" + toSell.get("id"))).andExpect(jsonPath("$.teamId").value(offer.get("teamId")));
        long budgetAfter = ((Number) JsonPath.read(body(get("/api/teams/" + teamId)), "$.budget")).longValue();
        assertEquals(budgetBefore + ((Number) offer.get("amount")).longValue(), budgetAfter);

        // Menajer modunda başka takım adına transfer yapılamaz
        int otherTeam = JsonPath.<List<Integer>>read(body(get("/api/teams")), "$[?(@.id != " + teamId + ")].id").getFirst();
        int freeAgent = JsonPath.<List<Integer>>read(body(get("/api/transfers/market")), "$[?(@.freeAgent == true)].playerId").getFirst();
        mockMvc.perform(post("/api/transfers/free-agents/" + freeAgent + "/sign").contentType(APPLICATION_JSON)
                .content("{\"teamId\": " + otherTeam + "}")).andExpect(status().isBadRequest());
    }

    private void createTeams() throws Exception {
        mockMvc.perform(post("/api/teams/random").contentType(APPLICATION_JSON).content("{\"count\": 14}"))
                .andExpect(status().isCreated());
    }

    private void startManaging(int teamId) throws Exception {
        mockMvc.perform(post("/api/my-team").contentType(APPLICATION_JSON)
                .content("{\"managerName\": \"Hoca\", \"teamId\": " + teamId + "}")).andExpect(status().isOk());
    }

    private String body(RequestBuilder request) throws Exception {
        return mockMvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }
}
