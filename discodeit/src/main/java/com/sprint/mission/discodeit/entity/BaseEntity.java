package com.sprint.mission.discodeit.entity;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public class BaseEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    protected UUID id;
    protected Instant createdAt;
    protected Instant updatedAt;

    public BaseEntity() {
        this.id = UUID.randomUUID();
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    protected void updateTimeStamp() {
        this.updatedAt = Instant.now();
    }
}