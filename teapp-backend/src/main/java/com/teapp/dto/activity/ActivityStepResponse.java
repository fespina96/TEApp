package com.teapp.dto.activity;

import java.util.UUID;

public record ActivityStepResponse(
    UUID id,
    int stepOrder,
    String title,
    String description,
    String imageBase64,
    String pictogramUrl
) {}
