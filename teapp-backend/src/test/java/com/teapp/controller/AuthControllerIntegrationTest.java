package com.teapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teapp.dto.auth.LoginRequest;
import com.teapp.dto.auth.RegisterRequest;
import com.teapp.enums.UserRole;
import com.teapp.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthController - Integración")
class AuthControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;

    private static final String BASE = "/api/v1/auth";

    // ─── POST /register ───────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /register: datos válidos → 200 con token")
    void register_validData_returns200WithToken() throws Exception {
        RegisterRequest req = new RegisterRequest("nuevo@test.com", "Pass1234", "Nuevo Padre", java.time.LocalDate.of(1990, 5, 15), UserRole.PARENT);

        mockMvc.perform(post(BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("nuevo@test.com"))
                .andExpect(jsonPath("$.role").value("PARENT"));
    }

    @Test
    @DisplayName("POST /register: email duplicado → 400")
    void register_duplicateEmail_returns400() throws Exception {
        RegisterRequest first = new RegisterRequest("dup@test.com", "Pass1234", "Padre A", java.time.LocalDate.of(1990,1,1), UserRole.PARENT);
        mockMvc.perform(post(BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(first)));

        RegisterRequest second = new RegisterRequest("dup@test.com", "Pass5678", "Padre B", java.time.LocalDate.of(1990,1,1), UserRole.PARENT);
        mockMvc.perform(post(BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /register: email inválido → 400 de validación")
    void register_invalidEmail_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest("no-es-email", "Pass1234", "Padre", java.time.LocalDate.of(1990,1,1), UserRole.PARENT);

        mockMvc.perform(post(BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /register: contraseña sin mayúscula → 400 de validación")
    void register_passwordWithoutUppercase_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest("padre@test.com", "pass1234", "Padre", java.time.LocalDate.of(1990,1,1), UserRole.PARENT);

        mockMvc.perform(post(BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /register: terapeuta → devuelve inviteCode de 8 caracteres")
    void register_therapist_returnsInviteCode() throws Exception {
        RegisterRequest req = new RegisterRequest("tera@test.com", "Pass1234", "Dra. Pérez", java.time.LocalDate.of(1985,6,20), UserRole.THERAPIST);

        mockMvc.perform(post(BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inviteCode").isNotEmpty())
                .andExpect(jsonPath("$.inviteCode", hasLength(8)));
    }

    // ─── POST /login ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /login: credenciales correctas → 200 con token")
    void login_validCredentials_returns200() throws Exception {
        // Registrar primero
        RegisterRequest reg = new RegisterRequest("login@test.com", "Pass1234", "Padre Login", java.time.LocalDate.of(1990,1,1), UserRole.PARENT);
        mockMvc.perform(post(BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)));

        LoginRequest login = new LoginRequest("login@test.com", "Pass1234");
        mockMvc.perform(post(BASE + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("login@test.com"));
    }

    @Test
    @DisplayName("POST /login: contraseña incorrecta → 401")
    void login_wrongPassword_returns401() throws Exception {
        RegisterRequest reg = new RegisterRequest("auth@test.com", "Pass1234", "Padre", java.time.LocalDate.of(1990,1,1), UserRole.PARENT);
        mockMvc.perform(post(BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)));

        LoginRequest login = new LoginRequest("auth@test.com", "WrongPass9");
        mockMvc.perform(post(BASE + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /login: usuario inexistente → 401")
    void login_unknownUser_returns401() throws Exception {
        LoginRequest login = new LoginRequest("noexiste@test.com", "Pass1234");

        mockMvc.perform(post(BASE + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }
}
