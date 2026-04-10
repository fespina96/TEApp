package com.teapp.dto.completion;

import java.time.LocalDate;
import java.util.UUID;

public record CompletionResponse(
    UUID id,
    UUID scheduleEntryId,
    LocalDate completedDate
) {}
