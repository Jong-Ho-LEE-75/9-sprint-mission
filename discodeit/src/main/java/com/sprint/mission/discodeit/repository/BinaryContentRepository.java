package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.entity.BinaryContent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ========================================
 * 바이너리 콘텐츠 저장소 인터페이스
 * ========================================
 *
 * 이 인터페이스는 BinaryContent(파일) 엔티티의 영속성을 담당합니다.
 * 프로필 이미지, 메시지 첨부파일 등의 저장/조회/삭제를 처리합니다.
 *
 * [구현체]
 * - JCFBinaryContentRepository: HashMap을 사용하여 메모리에 저장
 * - FileBinaryContentRepository: 파일 시스템에 직렬화하여 저장
 *
 * [특징]
 * 1. 파일 데이터는 크기가 클 수 있으므로 조회 시 주의
 * 2. findAllByIdIn()으로 여러 파일을 한 번에 조회 가능
 * 3. 실제 프로덕션에서는 AWS S3 같은 클라우드 스토리지 사용 권장
 */
public interface BinaryContentRepository {
    /**
     * 바이너리 콘텐츠(파일)를 저장합니다.
     *
     * BinaryContent는 불변 객체이므로 Update는 없고 Create만 있습니다.
     * 파일 수정 시에는 기존 파일 삭제 후 새 파일 저장합니다.
     *
     * @param binaryContent 저장할 바이너리 콘텐츠
     * @return 저장된 바이너리 콘텐츠
     */
    BinaryContent save(BinaryContent binaryContent);

    /**
     * ID로 바이너리 콘텐츠를 조회합니다.
     *
     * [사용 예시]
     * BinaryContent profile = binaryContentRepository.findById(user.getProfileId())
     *     .orElseThrow(() -> new NoSuchElementException("Profile not found"));
     *
     * @param id 바이너리 콘텐츠 ID
     * @return 조회된 바이너리 콘텐츠 (없으면 Optional.empty())
     */
    Optional<BinaryContent> findById(UUID id);

    /**
     * 모든 바이너리 콘텐츠를 조회합니다.
     *
     * 파일 데이터를 모두 메모리에 올리므로 주의해서 사용해야 합니다.
     * 대량의 파일이 있으면 OutOfMemoryError 발생 가능!
     *
     * @return 모든 바이너리 콘텐츠 목록
     */
    List<BinaryContent> findAll();

    /**
     * 여러 ID로 바이너리 콘텐츠 목록을 조회합니다.
     *
     * 메시지의 첨부파일 목록을 한 번에 조회할 때 유용합니다.
     *
     * [사용 예시]
     * List<UUID> attachmentIds = message.getAttachmentIds();
     * List<BinaryContent> attachments = binaryContentRepository.findAllByIdIn(attachmentIds);
     *
     * @param ids 바이너리 콘텐츠 ID 목록
     * @return 조회된 바이너리 콘텐츠 목록
     */
    List<BinaryContent> findAllByIdIn(List<UUID> ids);

    /**
     * ID로 바이너리 콘텐츠를 삭제합니다.
     *
     * @param id 삭제할 바이너리 콘텐츠 ID
     */
    void deleteById(UUID id);

    /**
     * ID로 바이너리 콘텐츠의 존재 여부를 확인합니다.
     *
     * @param id 바이너리 콘텐츠 ID
     * @return true: 존재함, false: 존재하지 않음
     */
    boolean existsById(UUID id);
}
