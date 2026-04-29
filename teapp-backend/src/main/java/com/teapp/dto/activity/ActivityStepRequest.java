package com.teapp.dto.activity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActivityStepRequest(
    @NotBlank(message = "El título del paso es obligatorio")
    @Size(max = 200)
    String title,

    @Size(max = 1000)
    String description,

    String imageBase64,
    String pictogramUrl
) {}
