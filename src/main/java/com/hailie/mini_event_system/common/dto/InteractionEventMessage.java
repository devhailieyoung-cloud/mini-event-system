package com.hailie.mini_event_system.common.dto;

import com.hailie.mini_event_system.domain.type.InteractionType;

import java.time.LocalDateTime;

public record InteractionEventMessage(
        String gameId,
        long memberNo,
        InteractionType interactionType,
        LocalDateTime timestamp
) {}
