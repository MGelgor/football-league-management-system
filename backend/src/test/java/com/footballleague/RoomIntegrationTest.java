package com.footballleague;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.footballleague.service.RoomService;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RoomIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RoomService roomService;

    private record Session(String code, String token) {
    }

    @Test
    void odaKurulunca18TakimlikKendiLigiOlurVeVarsayilanLigEtkilenmez() throws Exception {
        Session owner = createRoom("Cuma Ligi", "Ali");

        mockMvc.perform(get("/api/rooms/" + owner.code()).header("X-Room-Token", owner.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cuma Ligi"))
                .andExpect(jsonPath("$.members", hasSize(1)))
                .andExpect(jsonPath("$.members[0].owner").value(true));
        mockMvc.perform(in(get("/api/teams"), owner)).andExpect(jsonPath("$", hasSize(18)));
        mockMvc.perform(get("/api/teams")).andExpect(jsonPath("$", hasSize(4)));

        // Oda sahibi ligi başlatıp ilk maç gününü hemen oynatır; varsayılan ligde fikstür yok
        mockMvc.perform(post("/api/rooms/" + owner.code() + "/start").header("X-Room-Token", owner.token()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/rooms/" + owner.code() + "/run-now").header("X-Room-Token", owner.token()))
                .andExpect(status().isOk());
        // Maç günü yeni oynandı: canlı yayın bitene kadar sonuçlar kilitli
        mockMvc.perform(in(get("/api/matches/1"), owner)).andExpect(status().isConflict());
        mockMvc.perform(get("/api/matches/1")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/fixtures")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void ikiOdaBirbirininVerisiniGormez() throws Exception {
        Session first = createRoom("Birinci", "Ali");
        Session second = createRoom("İkinci", "Veli");

        mockMvc.perform(in(post("/api/teams/random"), first).contentType(APPLICATION_JSON).content("{\"count\":2}"))
                .andExpect(status().isCreated());
        mockMvc.perform(in(get("/api/teams"), first)).andExpect(jsonPath("$", hasSize(20)));
        mockMvc.perform(in(get("/api/teams"), second)).andExpect(jsonPath("$", hasSize(18)));

        mockMvc.perform(post("/api/rooms/" + first.code() + "/start").header("X-Room-Token", first.token()))
                .andExpect(status().isOk());
        mockMvc.perform(in(get("/api/fixtures"), first)).andExpect(jsonPath("$", hasSize(38)));
        mockMvc.perform(in(get("/api/fixtures"), second)).andExpect(jsonPath("$", hasSize(0)));
        // Başka odanın token'ı geçmez
        mockMvc.perform(get("/api/teams").header("X-Room-Code", second.code()).header("X-Room-Token", first.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void uyeOkuyabilirAmaYalnizcaSahipVeriDegistirebilir() throws Exception {
        Session owner = createRoom("Oda", "Ali");
        Session member = join(owner.code().toLowerCase(), "Veli");

        mockMvc.perform(in(get("/api/standings"), member)).andExpect(status().isOk());
        mockMvc.perform(in(post("/api/teams/random"), member).contentType(APPLICATION_JSON).content("{\"count\":2}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Bu işlemi yalnızca oda sahibi yapabilir"));
        mockMvc.perform(get("/api/teams").header("X-Room-Code", owner.code()).header("X-Room-Token", "yanlis"))
                .andExpect(status().isForbidden());
        // EventSource / indirme linkleri için sorgu parametreleri
        mockMvc.perform(get("/api/teams").param("room", owner.code()).param("token", member.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(18)));

        mockMvc.perform(post("/api/rooms/" + owner.code() + "/join").contentType(APPLICATION_JSON)
                .content("{\"nickname\":\"veli\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/rooms/YOKYOK/join").contentType(APPLICATION_JSON)
                .content("{\"nickname\":\"Can\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void uyeAyrilirSahipOdayiSiler() throws Exception {
        Session owner = createRoom("Oda", "Ali");
        Session member = join(owner.code(), "Veli");

        mockMvc.perform(delete("/api/rooms/" + owner.code()).header("X-Room-Token", member.token()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/rooms/" + owner.code() + "/members/me").header("X-Room-Token", owner.token()))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/rooms/" + owner.code() + "/members/me").header("X-Room-Token", member.token()))
                .andExpect(status().isNoContent());
        mockMvc.perform(in(get("/api/teams"), member)).andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/rooms/" + owner.code()).header("X-Room-Token", owner.token()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/rooms/" + owner.code()).header("X-Room-Token", owner.token()))
                .andExpect(status().isNotFound());
        mockMvc.perform(in(get("/api/teams"), owner)).andExpect(status().isForbidden());
    }

    @Test
    void odadaKayitNoktasiYokYedekOdaKayitlariniIcermez() throws Exception {
        Session owner = createRoom("Oda", "Ali");

        mockMvc.perform(in(get("/api/data/saves"), owner)).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(in(post("/api/data/saves"), owner).contentType(APPLICATION_JSON).content("{\"name\":\"a\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(in(get("/api/data/export"), owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.teamCount").value(18))
                .andExpect(jsonPath("$.tables.teams").exists())
                .andExpect(jsonPath("$.tables.rooms").doesNotExist());
        mockMvc.perform(get("/api/data/export")).andExpect(jsonPath("$.tables.room_members").doesNotExist());
    }

    @Test
    void acilistaOdaSemalariYeniSurumeGuncellenir() throws Exception {
        Session owner = createRoom("Oda", "Ali");
        String schema = "room_" + owner.code().toLowerCase();
        jdbcTemplate.execute("alter table " + schema + ".teams drop column logo_path");
        jdbcTemplate.execute("drop table " + schema + ".referees cascade");

        roomService.run(null);

        Integer columns = jdbcTemplate.queryForObject("select count(*) from information_schema.columns "
                + "where lower(table_schema) = ? and lower(table_name) = 'teams' and lower(column_name) = 'logo_path'",
                Integer.class, schema);
        assertEquals(1, columns);
        mockMvc.perform(in(get("/api/referees"), owner)).andExpect(status().isOk());
        mockMvc.perform(in(get("/api/teams"), owner)).andExpect(jsonPath("$", hasSize(18)));
    }

    private Session createRoom(String name, String nickname) throws Exception {
        String body = mockMvc.perform(post("/api/rooms").contentType(APPLICATION_JSON)
                .content("{\"roomName\":\"" + name + "\",\"nickname\":\"" + nickname + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new Session(JsonPath.read(body, "$.room.code"), JsonPath.read(body, "$.token"));
    }

    private Session join(String code, String nickname) throws Exception {
        String body = mockMvc.perform(post("/api/rooms/" + code + "/join").contentType(APPLICATION_JSON)
                .content("{\"nickname\":\"" + nickname + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new Session(JsonPath.read(body, "$.room.code"), JsonPath.read(body, "$.token"));
    }

    private static MockHttpServletRequestBuilder in(MockHttpServletRequestBuilder request, Session session) {
        return request.header("X-Room-Code", session.code()).header("X-Room-Token", session.token());
    }
}
