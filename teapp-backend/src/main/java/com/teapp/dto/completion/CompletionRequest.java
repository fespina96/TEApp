package com.teapp.dto.completion;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CompletionRequest(
    @NotNull(message = "La fecha es obligatoria")
    LocalDate date
) {}
