package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.entity.BinaryContent;
import java.util.List;
import java.util.UUID;

/**
 * ========================================
 * 바이너리 콘텐츠 서비스 인터페이스
 * ========================================
 *
 * 이 인터페이스는 파일(바이너리 콘텐츠) 관련 비즈니스 로직을 정의합니다.
 * 파일 업로드, 조회, 삭제 기능을 제공합니다.
 *
 * [용도]
 * - 프로필 이미지 저장/조회
 * - 메시지 첨부파일 저장/조회
 *
 * [특징]
 * BinaryContent는 불변(Immutable) 객체이므로 Update는 없고
 * Create와 Delete만 있습니다.
 * 파일 수정 시에는 기존 파일 삭제 → 새 파일 저장 방식을 사용합니다.
 *
 * [구현체]
 * BasicBinaryContentService: 기본 바이너리 콘텐츠 서비스 구현
 */
public interface BinaryContentService {
    /**
     * 바이너리 콘텐츠(파일)를 생성합니다.
     *
     * @param request 파일 생성 요청 (fileName, contentType, data)
     * @return 생성된 BinaryContent 엔티티
     */
    BinaryContent create(BinaryContentCreateRequest request);

    /**
     * ID로 바이너리 콘텐츠를 조회합니다.
     *
     * @param id 바이너리 콘텐츠 ID
     * @return 조회된 BinaryContent 엔티티
     * @throws java.util.NoSuchElementException 파일이 없을 경우
     */
    BinaryContent find(UUID id);

    /**
     * 여러 ID로 바이너리 콘텐츠 목록을 조회합니다.
     *
     * 메시지의 첨부파일 목록을 한 번에 조회할 때 유용합니다.
     *
     * @param ids 바이너리 콘텐츠 ID 목록
     * @return 조회된 BinaryContent 목록
     */
    List<BinaryContent> findAllByIdIn(List<UUID> ids);

    /**
     * 바이너리 콘텐츠를 삭제합니다.
     *
     * @param id 삭제할 바이너리 콘텐츠 ID
     */
    void delete(UUID id);
}
