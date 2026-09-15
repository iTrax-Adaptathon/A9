package com.syncslot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncslot.dto.AiExplainRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that the AI narration endpoint degrades to the deterministic engine
 * explanation when no API key is configured, and that it never fails the request.
 */
@SpringBootTest(properties = "app.ai.api-key=")
@AutoConfigureMockMvc
class AiExplainApiTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @Test
    void explainFallsBackToDeterministicTextWhenAiDisabled() throws Exception {
        AiExplainRequest request = new AiExplainRequest(
                "Executive Review",
                List.of("Alice", "Bob", "Charlie"),
                60,
                "URGENT",
                "10:00-11:00",
                "Team Catchup",
                "LOW",
                "Move Team Catchup to 13:00-14:00",
                0,
                true,
                "10:00-11:00 was selected because all participants are available.");

        mockMvc.perform(post("/api/ai/explain")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiGenerated").value(false))
                .andExpect(jsonPath("$.explanation")
                        .value("10:00-11:00 was selected because all participants are available."));
    }

    @Test
    void explainHandlesEmptyRequest() throws Exception {
        mockMvc.perform(post("/api/ai/explain")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiGenerated").value(false))
                .andExpect(jsonPath("$.explanation").isNotEmpty());
    }
}
