package com.sprint.mission.discodeit.entity;

import lombok.Getter;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * 바이너리 파일 콘텐츠를 저장하는 엔티티 클래스
 * 이미지, PDF 등의 바이너리 데이터를 저장하며, 불변(immutable) 객체입니다.
 */
@Getter
public class BinaryContent implements Serializable {
    /** 직렬화 버전 UID */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 바이너리 콘텐츠 고유 식별자 */
    private final UUID id;

    /** 생성 일시 */
    private final Instant createdAt;

    /** 파일명 */
    private final String fileName;

    /** MIME 타입 (예: image/png, application/pdf) */
    private final String contentType;

    /** 실제 바이너리 데이터 */
    private final byte[] data;

    /**
     * 바이너리 콘텐츠 생성자
     *
     * @param fileName 파일명
     * @param contentType MIME 타입
     * @param data 실제 바이너리 데이터
     */
    public BinaryContent(String fileName, String contentType, byte[] data) {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.fileName = fileName;
        this.contentType = contentType;
        this.data = data;
    }

    /**
     * 고정 UUID를 사용하는 바이너리 콘텐츠 생성자 (테스트 및 초기 데이터용)
     *
     * @param id 고정 UUID
     * @param fileName 파일명
     * @param contentType MIME 타입
     * @param data 실제 바이너리 데이터
     */
    public BinaryContent(UUID id, String fileName, String contentType, byte[] data) {
        this.id = id;
        this.createdAt = Instant.now();
        this.fileName = fileName;
        this.contentType = contentType;
        this.data = data;
    }
}