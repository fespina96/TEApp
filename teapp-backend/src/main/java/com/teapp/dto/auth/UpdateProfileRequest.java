package com.teapp.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateProfileRequest(
    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 255)
    String fullName,

    @Past(message = "La fecha de nacimiento debe ser en el pasado")
    LocalDate dateOfBirth
) {}
