package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.entity.Message;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ========================================
 * 메시지 저장소 인터페이스
 * ========================================
 *
 * 이 인터페이스는 Message 엔티티의 영속성(저장/조회/삭제)을 담당합니다.
 *
 * [구현체]
 * - JCFMessageRepository: HashMap을 사용하여 메모리에 저장
 * - FileMessageRepository: 파일 시스템에 직렬화하여 저장
 *
 * [특징]
 * 메시지는 채널에 속하므로 채널 ID로 조회하는 기능이 있습니다.
 * 채널 삭제 시 해당 채널의 모든 메시지를 일괄 삭제하는 기능도 있습니다.
 */
public interface MessageRepository {
    /**
     * 메시지를 저장합니다.
     *
     * Create와 Update 모두에 사용됩니다.
     *
     * @param message 저장할 메시지 엔티티
     * @return 저장된 메시지 엔티티
     */
    Message save(Message message);

    /**
     * ID로 메시지를 조회합니다.
     *
     * @param id 메시지 ID (UUID)
     * @return 조회된 메시지 (없으면 Optional.empty())
     */
    Optional<Message> findById(UUID id);

    /**
     * 모든 메시지를 조회합니다.
     *
     * @return 모든 메시지 목록
     */
    List<Message> findAll();

    /**
     * 특정 채널의 모든 메시지를 조회합니다.
     *
     * 채널 화면에서 메시지 목록을 표시할 때 사용합니다.
     *
     * [사용 예시]
     * List<Message> messages = messageRepository.findAllByChannelId(channelId);
     * // 시간순 정렬, 페이징 등은 Service에서 처리
     *
     * @param channelId 채널 ID
     * @return 해당 채널의 메시지 목록 (없으면 빈 리스트)
     */
    List<Message> findAllByChannelId(UUID channelId);

    /**
     * ID로 메시지를 삭제합니다.
     *
     * 메시지 삭제 시 첨부파일(BinaryContent)도
     * Service에서 함께 삭제해야 합니다.
     *
     * @param id 삭제할 메시지 ID
     */
    void deleteById(UUID id);

    /**
     * 특정 채널의 모든 메시지를 일괄 삭제합니다.
     *
     * 채널 삭제 시 해당 채널의 모든 메시지를 삭제할 때 사용합니다.
     * 첨부파일 삭제는 Service에서 별도로 처리해야 합니다.
     *
     * @param channelId 채널 ID
     */
    void deleteAllByChannelId(UUID channelId);

    /**
     * ID로 메시지의 존재 여부를 확인합니다.
     *
     * @param id 메시지 ID
     * @return true: 존재함, false: 존재하지 않음
     */
    boolean existsById(UUID id);
}
