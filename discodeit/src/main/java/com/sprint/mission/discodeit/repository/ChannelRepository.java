package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.entity.Channel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ========================================
 * 채널 저장소 인터페이스
 * ========================================
 *
 * 이 인터페이스는 Channel 엔티티의 영속성(저장/조회/삭제)을 담당합니다.
 *
 * [구현체]
 * - JCFChannelRepository: HashMap을 사용하여 메모리에 저장
 * - FileChannelRepository: 파일 시스템에 직렬화하여 저장
 *
 * [특징]
 * Channel은 PUBLIC과 PRIVATE 두 가지 타입이 있지만,
 * Repository에서는 타입을 구분하지 않고 동일하게 처리합니다.
 * 타입에 따른 로직 분기는 Service 레이어에서 처리합니다.
 */
public interface ChannelRepository {
    /**
     * 채널을 저장합니다.
     *
     * Create와 Update 모두에 사용됩니다.
     *
     * @param channel 저장할 채널 엔티티
     * @return 저장된 채널 엔티티
     */
    Channel save(Channel channel);

    /**
     * ID로 채널을 조회합니다.
     *
     * @param id 채널 ID (UUID)
     * @return 조회된 채널 (없으면 Optional.empty())
     */
    Optional<Channel> findById(UUID id);

    /**
     * 모든 채널을 조회합니다.
     *
     * PUBLIC, PRIVATE 구분 없이 모든 채널을 반환합니다.
     * 특정 사용자가 접근 가능한 채널만 필터링하는 것은
     * Service 레이어(ChannelService.findAllByUserId)에서 처리합니다.
     *
     * @return 모든 채널 목록
     */
    List<Channel> findAll();

    /**
     * ID로 채널을 삭제합니다.
     *
     * 채널 삭제 시 연관된 데이터(Message, ReadStatus)도
     * Service에서 먼저 삭제해야 합니다.
     *
     * @param id 삭제할 채널 ID
     */
    void deleteById(UUID id);

    /**
     * ID로 채널의 존재 여부를 확인합니다.
     *
     * @param id 채널 ID
     * @return true: 존재함, false: 존재하지 않음
     */
    boolean existsById(UUID id);
}
