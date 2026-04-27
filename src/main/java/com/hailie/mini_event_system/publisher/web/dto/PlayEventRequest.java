package com.hailie.mini_event_system.publisher.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record PlayEventRequest(
        @NotBlank String gameId,
        @Positive long memberNo
) {}
