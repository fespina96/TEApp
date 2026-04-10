package com.teapp.dto.child;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ChildResponse(
    UUID id,
    String name,
    LocalDate dateOfBirth,
    String avatarColor,
    String avatarBase64,
    String notes,
    LocalDateTime createdAt
) {}
