package com.sprint.mission.discodeit.entity;

import lombok.Getter;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * ========================================
 * 바이너리 콘텐츠 엔티티 클래스
 * ========================================
 *
 * 이 클래스는 이미지, PDF 등의 파일 데이터를 저장합니다.
 * 프로필 이미지, 메시지 첨부파일 등에 사용됩니다.
 *
 * [바이너리 데이터란?]
 * 텍스트가 아닌 이진 형식의 데이터입니다.
 * 이미지, 동영상, PDF, 실행 파일 등이 바이너리 데이터입니다.
 * 예: PNG 이미지는 0과 1로 이루어진 바이트 데이터로 저장됨
 *
 * [MIME 타입이란?]
 * 파일의 형식을 나타내는 표준 방식입니다.
 * 형식: 타입/서브타입
 * 예:
 * - image/png: PNG 이미지
 * - image/jpeg: JPEG 이미지
 * - application/pdf: PDF 문서
 * - text/plain: 텍스트 파일
 *
 * [불변(Immutable) 객체란?]
 * 한 번 생성되면 내부 상태를 변경할 수 없는 객체입니다.
 * BinaryContent는 모든 필드가 final로 선언되어 불변 객체입니다.
 *
 * [왜 불변 객체로 설계했나요?]
 * 1. 파일 데이터는 수정보다 교체가 일반적 (새 파일 업로드)
 * 2. 불변 객체는 스레드 안전(Thread-Safe)
 * 3. 의도치 않은 데이터 변경 방지
 *
 * [BaseEntity를 상속받지 않는 이유]
 * BinaryContent는 updatedAt이 필요 없고 (불변이므로),
 * 다른 엔티티와 구조가 다르기 때문입니다.
 *
 * [어노테이션 설명]
 * @Getter (Lombok): 모든 필드에 대한 getter 메서드 자동 생성
 */
@Getter
public class BinaryContent implements Serializable {
    /**
     * 직렬화 버전 UID
     *
     * Serializable 인터페이스 구현 시 권장되는 필드입니다.
     * 클래스 버전 관리에 사용됩니다.
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 바이너리 콘텐츠 고유 식별자
     *
     * 파일을 식별하는 고유 ID입니다.
     * User의 profileId나 Message의 attachmentIds가 이 ID를 참조합니다.
     */
    private final UUID id;

    /**
     * 생성 일시
     *
     * 파일이 업로드된 시간입니다.
     * 불변 객체이므로 수정일시(updatedAt)는 없습니다.
     */
    private final Instant createdAt;

    /**
     * 파일명
     *
     * 업로드된 파일의 원래 이름입니다.
     * 다운로드 시 이 이름으로 저장됩니다.
     *
     * 예: "profile.png", "document.pdf", "photo.jpg"
     */
    private final String fileName;

    /**
     * MIME 타입 (Content-Type)
     *
     * 파일의 형식을 나타냅니다.
     * 브라우저가 파일을 어떻게 처리할지 결정하는 데 사용됩니다.
     *
     * [일반적인 MIME 타입]
     * - image/png: PNG 이미지 (투명 배경 지원)
     * - image/jpeg: JPEG 이미지 (사진에 적합)
     * - image/gif: GIF 이미지 (애니메이션 지원)
     * - application/pdf: PDF 문서
     * - text/plain: 텍스트 파일
     * - application/json: JSON 데이터
     *
     * 예: "image/png", "application/pdf"
     */
    private final String contentType;

    /**
     * 실제 파일 데이터 (바이트 배열)
     *
     * 파일의 실제 내용을 바이트 배열로 저장합니다.
     *
     * [byte[]란?]
     * 바이트(byte)의 배열입니다.
     * 1바이트 = 8비트 = 0~255 사이의 값
     * 모든 파일은 궁극적으로 바이트의 나열로 표현됩니다.
     *
     * [주의사항]
     * 파일 크기가 크면 메모리를 많이 차지합니다.
     * 실제 프로덕션에서는 파일 시스템이나 클라우드 스토리지
     * (AWS S3 등)에 저장하는 것이 좋습니다.
     *
     * 예: 100KB 이미지 → 약 102,400개의 바이트
     */
    private final byte[] data;

    /**
     * ========================================
     * 바이너리 콘텐츠 생성자
     * ========================================
     *
     * 새 파일을 업로드할 때 사용합니다.
     * ID와 생성일시는 자동으로 설정됩니다.
     *
     * [사용 예시]
     * // 이미지 파일 생성
     * byte[] imageData = Files.readAllBytes(Paths.get("photo.png"));
     * BinaryContent image = new BinaryContent(
     *     "photo.png",
     *     "image/png",
     *     imageData
     * );
     *
     * // PDF 파일 생성
     * byte[] pdfData = Files.readAllBytes(Paths.get("document.pdf"));
     * BinaryContent pdf = new BinaryContent(
     *     "document.pdf",
     *     "application/pdf",
     *     pdfData
     * );
     *
     * @param fileName    파일명 (예: "photo.png")
     * @param contentType MIME 타입 (예: "image/png")
     * @param data        파일 데이터 (바이트 배열)
     */
    public BinaryContent(String fileName, String contentType, byte[] data) {
        // UUID 자동 생성
        this.id = UUID.randomUUID();
        // 현재 시간으로 생성일시 설정
        this.createdAt = Instant.now();
        // 파일 정보 설정
        this.fileName = fileName;
        this.contentType = contentType;
        this.data = data;
    }
}
