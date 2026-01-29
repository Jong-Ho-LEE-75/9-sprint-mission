package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.service.BinaryContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * ========================================
 * BinaryContentService 인터페이스의 기본 구현체
 * ========================================
 *
 * 이 클래스는 바이너리 콘텐츠(파일) 관련 비즈니스 로직을 구현합니다.
 * 파일 업로드, 조회, 삭제 기능을 제공합니다.
 *
 * [단순한 구현]
 * BinaryContent는 불변 객체이고, 복잡한 비즈니스 로직이 없어서
 * Repository 호출을 단순히 위임하는 형태입니다.
 *
 * [왜 Service를 거치나요?]
 * 지금은 단순하지만, 나중에 다음과 같은 로직이 추가될 수 있습니다:
 * - 파일 크기 검증
 * - 허용된 파일 형식 검증
 * - 바이러스 검사
 * - 이미지 리사이징
 * - 클라우드 스토리지 연동
 *
 * [어노테이션 설명]
 * @Service: Spring Bean으로 등록
 * @RequiredArgsConstructor: final 필드에 대한 생성자 자동 생성
 */
@Service
@RequiredArgsConstructor
public class BasicBinaryContentService implements BinaryContentService {
    /**
     * 바이너리 콘텐츠를 저장하고 조회하는 리포지토리
     */
    private final BinaryContentRepository binaryContentRepository;

    /**
     * 바이너리 콘텐츠(파일)를 생성합니다.
     *
     * Request DTO를 Entity로 변환하여 저장합니다.
     */
    @Override
    public BinaryContent create(BinaryContentCreateRequest request) {
        BinaryContent binaryContent = new BinaryContent(
                request.fileName(),
                request.contentType(),
                request.data()
        );
        return binaryContentRepository.save(binaryContent);
    }

    @Override
    public BinaryContent find(UUID id) {
        return binaryContentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("BinaryContent not found: " + id));
    }

    /**
     * 여러 ID로 바이너리 콘텐츠 목록을 조회합니다.
     *
     * 메시지의 첨부파일 목록을 한 번에 조회할 때 유용합니다.
     */
    @Override
    public List<BinaryContent> findAllByIdIn(List<UUID> ids) {
        return binaryContentRepository.findAllByIdIn(ids);
    }

    @Override
    public void delete(UUID id) {
        binaryContentRepository.deleteById(id);
    }
}
