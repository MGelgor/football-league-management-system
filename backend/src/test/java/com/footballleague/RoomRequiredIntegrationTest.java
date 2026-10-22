package com.footballleague;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest(properties = "app.rooms.required=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext
class RoomRequiredIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void odaZorunluykenOdasizIstekReddedilir() throws Exception {
        mockMvc.perform(get("/api/teams")).andExpect(status().isUnauthorized());

        String body = mockMvc.perform(post("/api/rooms").contentType(MediaType.APPLICATION_JSON)
                .content("{\"roomName\":\"Oda\",\"nickname\":\"Ali\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        mockMvc.perform(get("/api/teams")
                .header("X-Room-Code", JsonPath.<String>read(body, "$.room.code"))
                .header("X-Room-Token", JsonPath.<String>read(body, "$.token")))
                .andExpect(status().isOk());
    }
}
