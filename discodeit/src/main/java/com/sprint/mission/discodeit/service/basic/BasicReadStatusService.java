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
     *
     * [코드에서 사용된 메서드 설명]
     *
     * 1. existsById(id) - boolean 반환
     *    데이터 존재 여부만 확인합니다.
     *    - 존재하면: true
     *    - 없으면: false
     *    findById보다 가볍습니다 (데이터를 가져오지 않고 존재만 확인).
     *
     * 2. isPresent() - Optional의 메서드
     *    Optional에 값이 있는지 확인합니다.
     *    - Optional.of(값): isPresent() → true
     *    - Optional.empty(): isPresent() → false
     *
     *    [isPresent vs orElseThrow]
     *    - isPresent(): "있는지 없는지만 확인하고 싶을 때"
     *    - orElseThrow(): "값을 꺼내고 싶을 때 (없으면 예외)"
     *
     *    // isPresent 예시 - 중복 체크
     *    if (repository.findByEmail(email).isPresent()) {
     *        throw new Exception("이미 존재합니다");
     *    }
     *
     *    // orElseThrow 예시 - 값 조회
     *    User user = repository.findById(id)
     *        .orElseThrow(() -> new Exception("없습니다"));
     */
    @Override
    public ReadStatus create(ReadStatusCreateRequest request) {
        // 1단계: User 존재 여부 확인
        // existsById(): ID로 데이터가 존재하는지만 확인 (true/false)
        if (!userRepository.existsById(request.userId())) {
            throw new NoSuchElementException("User not found: " + request.userId());
        }

        // 2단계: Channel 존재 여부 확인
        if (!channelRepository.existsById(request.channelId())) {
            throw new NoSuchElementException("Channel not found: " + request.channelId());
        }

        // 3단계: 중복 체크
        // userId + channelId 조합은 유일해야 함
        // isPresent(): Optional에 값이 있으면 true (= 이미 존재함)
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

    /**
     * ID로 읽기 상태를 조회합니다.
     *
     * ※ Optional/orElseThrow 상세 설명은 BasicBinaryContentService.find() 참조
     *
     * @param id 읽기 상태 ID
     * @return 조회된 ReadStatus 엔티티
     * @throws NoSuchElementException 해당 ID의 ReadStatus가 없을 경우
     */
    @Override
    public ReadStatus find(UUID id) {
        return readStatusRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("ReadStatus not found: " + id));
    }

    /**
     * 특정 사용자의 모든 읽기 상태를 조회합니다.
     *
     * [용도]
     * - 사용자가 참여 중인 PRIVATE 채널 목록 조회
     * - 각 채널의 안 읽은 메시지 수 계산
     *
     * @param userId 사용자 ID
     * @return 해당 사용자의 ReadStatus 목록 (없으면 빈 리스트)
     */
    @Override
    public List<ReadStatus> findAllByUserId(UUID userId) {
        return readStatusRepository.findAllByUserId(userId);
    }

    /**
     * 읽기 상태를 업데이트합니다.
     *
     * 사용자가 채널에 입장하거나 메시지를 읽을 때 호출합니다.
     *
     * [조회 → 수정 → 저장 패턴]
     *
     * @param id 읽기 상태 ID
     * @param request 수정 요청 (lastReadAt)
     * @return 수정된 ReadStatus 엔티티
     * @throws NoSuchElementException 해당 ID의 ReadStatus가 없을 경우
     */
    @Override
    public ReadStatus update(UUID id, ReadStatusUpdateRequest request) {
        ReadStatus readStatus = readStatusRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("ReadStatus not found: " + id));

        readStatus.update(request.lastReadAt());
        return readStatusRepository.save(readStatus);
    }

    /**
     * 읽기 상태를 삭제합니다.
     *
     * [삭제 시점]
     * - 사용자가 PRIVATE 채널에서 나갈 때
     * - 채널이 삭제될 때 (관련 ReadStatus 일괄 삭제)
     *
     * @param id 삭제할 읽기 상태 ID
     */
    @Override
    public void delete(UUID id) {
        readStatusRepository.deleteById(id);
    }
}
