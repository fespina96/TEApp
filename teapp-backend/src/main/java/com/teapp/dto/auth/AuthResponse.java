package com.teapp.dto.auth;

import java.time.LocalDate;
import java.util.UUID;

public record AuthResponse(
    String token,
    long expiresIn,
    UUID id,
    String email,
    String fullName,
    String avatarBase64,
    String role,
    String inviteCode,
    LocalDate dateOfBirth
) {}
