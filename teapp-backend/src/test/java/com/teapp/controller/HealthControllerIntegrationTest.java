package com.teapp.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("HealthController - Integración")
class HealthControllerIntegrationTest {

    @Autowired MockMvc mockMvc;

    private static final String URL = "/api/v1/health";

    @Test
    @DisplayName("GET /health: sin token → 200 con {status:ok}")
    void health_noAuth_returns200WithStatusOk() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    @DisplayName("GET /health: con token inválido → 200 igualmente (endpoint público)")
    void health_withInvalidToken_stillReturns200() throws Exception {
        mockMvc.perform(get(URL)
                .header("Authorization", "Bearer token.invalido.xxx"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }
}
