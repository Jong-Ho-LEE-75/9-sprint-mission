package com.sprint.mission.discodeit.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        UUID profileId,
        boolean online,
        Instant createdAt,
        Instant updatedAt
) {}
