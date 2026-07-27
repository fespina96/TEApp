package com.teapp.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("GlobalExceptionHandler - Integración")
class GlobalExceptionHandlerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String registrarYObtenerToken(String email) throws Exception {
        String cuerpo = objectMapper.writeValueAsString(Map.of(
                "email", email,
                "password", "Test1234",
                "fullName", "Usuario Prueba",
                "dateOfBirth", "1990-01-01",
                "role", "PARENT"
        ));
        String respuesta = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(respuesta).get("token").asText();
    }

    @Test
    @DisplayName("Categoría de actividad inexistente → 400, no 500")
    void categoriaInvalida_devuelve400() throws Exception {
        String token = registrarYObtenerToken("categoria@test.com");
        String cuerpo = """
            {"name":"Prueba","category":"CHORES","color":"#A8D8EA"}
            """;

        mockMvc.perform(post("/api/v1/activities")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("JSON malformado → 400, no 500")
    void jsonMalformado_devuelve400() throws Exception {
        String token = registrarYObtenerToken("malformado@test.com");

        mockMvc.perform(post("/api/v1/activities")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\": "))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Categoría válida del enum → se acepta")
    void categoriaValida_seAcepta() throws Exception {
        String token = registrarYObtenerToken("outdoor@test.com");
        String cuerpo = """
            {"name":"Paseo","category":"OUTDOOR","color":"#C8E8D0"}
            """;

        mockMvc.perform(post("/api/v1/activities")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated());
    }
}
