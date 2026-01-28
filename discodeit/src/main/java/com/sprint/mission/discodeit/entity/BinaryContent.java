package com.sprint.mission.discodeit.entity;

import lombok.Getter;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Getter
public class BinaryContent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final Instant createdAt;

    private final String fileName;      // 파일명
    private final String contentType;   // MIME 타입 (예: image/png, application/pdf)
    private final byte[] data;          // 실제 바이너리 데이터

    public BinaryContent(String fileName, String contentType, byte[] data) {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.fileName = fileName;
        this.contentType = contentType;
        this.data = data;
    }
}