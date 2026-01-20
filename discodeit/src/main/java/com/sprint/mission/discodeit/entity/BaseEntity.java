package com.sprint.mission.discodeit.entity;

import java.io.Serializable;
import java.util.UUID;

public class BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    protected UUID id;
    protected Long createdAt;
    protected Long updatedAt;

    public BaseEntity() {
        this.id = UUID.randomUUID(); // 생성 시 자동으로 UUID 부여
        Long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public Long getCreatedAt() { return createdAt; }
    public Long getUpdatedAt() { return updatedAt; }

    protected void updateTimeStamp() {
        this.updatedAt = System.currentTimeMillis();
    }
}