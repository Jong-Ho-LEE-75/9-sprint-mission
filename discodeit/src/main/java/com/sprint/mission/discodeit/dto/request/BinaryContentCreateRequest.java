package com.sprint.mission.discodeit.dto.request;

/**
 * ========================================
 * 바이너리 콘텐츠(파일) 생성 요청 DTO
 * ========================================
 *
 * 이 record는 파일 업로드 시 클라이언트에서 서버로 전달되는 데이터를 담습니다.
 * 프로필 이미지, 메시지 첨부파일 등에 사용됩니다.
 *
 * [파일 업로드 과정]
 * 1. 클라이언트에서 파일 선택
 * 2. 파일을 바이트 배열로 변환
 * 3. BinaryContentCreateRequest 생성하여 서버로 전송
 * 4. 서버에서 BinaryContent 엔티티로 변환하여 저장
 *
 * [MIME 타입(Content-Type) 예시]
 * - image/png: PNG 이미지
 * - image/jpeg, image/jpg: JPEG 이미지
 * - image/gif: GIF 이미지 (애니메이션 가능)
 * - application/pdf: PDF 문서
 * - text/plain: 텍스트 파일
 * - application/zip: ZIP 압축 파일
 *
 * [사용 예시]
 * // 프로필 이미지 업로드
 * byte[] imageData = Files.readAllBytes(Paths.get("photo.png"));
 * BinaryContentCreateRequest request = new BinaryContentCreateRequest(
 *     "photo.png",
 *     "image/png",
 *     imageData
 * );
 * userService.create(userRequest, request);
 *
 * @param fileName    파일명 (예: "photo.png", "document.pdf")
 * @param contentType MIME 타입 (예: "image/jpeg", "application/pdf")
 * @param data        실제 파일 데이터 (바이트 배열)
 */
public record BinaryContentCreateRequest(
        String fileName,
        String contentType,
        byte[] data
) {
}
