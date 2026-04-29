package com.teapp.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateProfileRequest(
    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 255)
    String fullName,
    LocalDate dateOfBirth
) {}
