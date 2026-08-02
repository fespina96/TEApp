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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    @DisplayName("Participante con fecha de nacimiento futura → 400")
    void participanteConFechaFutura_devuelve400() throws Exception {
        String token = registrarYObtenerToken("fechaninio@test.com");
        String manana = java.time.LocalDate.now().plusDays(1).toString();
        String cuerpo = objectMapper.writeValueAsString(Map.of(
                "name", "Sofía", "dateOfBirth", manana, "avatarColor", "#A8D8EA"));

        mockMvc.perform(post("/api/v1/children")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Registro con fecha de nacimiento futura → 400")
    void registroConFechaFutura_devuelve400() throws Exception {
        String manana = java.time.LocalDate.now().plusDays(1).toString();
        String cuerpo = objectMapper.writeValueAsString(Map.of(
                "email", "futuro@test.com", "password", "Test1234",
                "fullName", "Usuario Futuro", "dateOfBirth", manana, "role", "PARENT"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Duración negativa o desmedida → 400")
    void duracionFueraDeRango_devuelve400() throws Exception {
        String token = registrarYObtenerToken("duracion@test.com");

        for (int minutos : new int[]{-30, 0, 999999}) {
            String cuerpo = objectMapper.writeValueAsString(Map.of(
                    "name", "Prueba", "category", "PLAY", "color", "#A8D8EA",
                    "durationMinutes", minutos));

            mockMvc.perform(post("/api/v1/activities")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    @DisplayName("Duración dentro del rango permitido → se acepta")
    void duracionValida_seAcepta() throws Exception {
        String token = registrarYObtenerToken("duracionok@test.com");
        String cuerpo = objectMapper.writeValueAsString(Map.of(
                "name", "Prueba", "category", "PLAY", "color", "#A8D8EA",
                "durationMinutes", 45));

        mockMvc.perform(post("/api/v1/activities")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Editar una entrada con duración fuera de rango → 400")
    void editarEntradaConDuracionInvalida_devuelve400() throws Exception {
        String token = registrarYObtenerToken("editar@test.com");

        // Participante y actividad propios para poder crear la entrada
        String childId = objectMapper.readTree(mockMvc.perform(post("/api/v1/children")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Sofía", "dateOfBirth", "2019-05-10", "avatarColor", "#A8D8EA"))))
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        String activityId = objectMapper.readTree(mockMvc.perform(post("/api/v1/activities")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Leer", "category", "PLAY", "color", "#A8D8EA"))))
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        String entryId = objectMapper.readTree(mockMvc.perform(post("/api/v1/children/" + childId + "/schedule")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "activityId", activityId, "dayOfWeek", "MONDAY", "timeSlot", "MORNING"))))
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        // Actualización parcial inválida: sólo la duración, fuera de rango
        mockMvc.perform(put("/api/v1/children/" + childId + "/schedule/" + entryId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"durationMinutes\":-15}"))
                .andExpect(status().isBadRequest());

        // Actualización parcial válida: se guarda y ya no se ignoran estos campos
        mockMvc.perform(put("/api/v1/children/" + childId + "/schedule/" + entryId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"durationMinutes\":20,\"requireFullTimer\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durationMinutes").value(20))
                .andExpect(jsonPath("$.requireFullTimer").value(true));
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
