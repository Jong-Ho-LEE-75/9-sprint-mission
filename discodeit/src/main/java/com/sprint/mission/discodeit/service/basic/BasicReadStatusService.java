package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.request.ReadStatusCreateRequest;
import com.sprint.mission.discodeit.dto.request.ReadStatusUpdateRequest;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.ReadStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * ========================================
 * ReadStatusService 인터페이스의 기본 구현체
 * ========================================
 *
 * 이 클래스는 읽기 상태 관련 비즈니스 로직을 구현합니다.
 * 사용자가 채널의 메시지를 어디까지 읽었는지 추적합니다.
 *
 * [의존하는 Repository]
 * - ReadStatusRepository: 읽기 상태 데이터 저장/조회
 * - UserRepository: 사용자 존재 여부 확인
 * - ChannelRepository: 채널 존재 여부 확인
 *
 * [어노테이션 설명]
 * @Service: Spring Bean으로 등록
 * @RequiredArgsConstructor: final 필드에 대한 생성자 자동 생성
 */
@Service
@RequiredArgsConstructor
public class BasicReadStatusService implements ReadStatusService {
    /**
     * 읽기 상태 정보를 저장하고 조회하는 리포지토리
     */
    private final ReadStatusRepository readStatusRepository;

    /**
     * 사용자 존재 여부 확인을 위한 리포지토리
     */
    private final UserRepository userRepository;

    /**
     * 채널 존재 여부 확인을 위한 리포지토리
     */
    private final ChannelRepository channelRepository;

    /**
     * ========================================
     * 읽기 상태를 생성합니다.
     * ========================================
     *
     * [검증 사항]
     * 1. userId의 User가 존재하는지 확인
     * 2. channelId의 Channel이 존재하는지 확인
     * 3. 같은 userId + channelId 조합이 이미 있는지 확인 (중복 방지)
     */
    @Override
    public ReadStatus create(ReadStatusCreateRequest request) {
        // 1단계: User 존재 여부 확인
        if (!userRepository.existsById(request.userId())) {
            throw new NoSuchElementException("User not found: " + request.userId());
        }

        // 2단계: Channel 존재 여부 확인
        if (!channelRepository.existsById(request.channelId())) {
            throw new NoSuchElementException("Channel not found: " + request.channelId());
        }

        // 3단계: 중복 체크
        // userId + channelId 조합은 유일해야 함
        if (readStatusRepository.findByUserIdAndChannelId(request.userId(), request.channelId()).isPresent()) {
            throw new IllegalArgumentException("ReadStatus already exists for user and channel");
        }

        // ReadStatus 생성 및 저장
        ReadStatus readStatus = new ReadStatus(
                request.userId(),
                request.channelId(),
                request.lastReadAt()
        );
        return readStatusRepository.save(readStatus);
    }

    @Override
    public ReadStatus find(UUID id) {
        return readStatusRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("ReadStatus not found: " + id));
    }

    @Override
    public List<ReadStatus> findAllByUserId(UUID userId) {
        return readStatusRepository.findAllByUserId(userId);
    }

    /**
     * 읽기 상태를 업데이트합니다.
     *
     * 사용자가 채널에 입장하거나 메시지를 읽을 때 호출합니다.
     */
    @Override
    public ReadStatus update(UUID id, ReadStatusUpdateRequest request) {
        ReadStatus readStatus = readStatusRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("ReadStatus not found: " + id));

        // 마지막 읽은 시간 갱신
        readStatus.update(request.lastReadAt());
        return readStatusRepository.save(readStatus);
    }

    @Override
    public void delete(UUID id) {
        readStatusRepository.deleteById(id);
    }
}
