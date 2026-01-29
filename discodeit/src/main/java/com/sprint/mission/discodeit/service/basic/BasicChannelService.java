package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.request.ChannelUpdateRequest;
import com.sprint.mission.discodeit.dto.request.PrivateChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.PublicChannelCreateRequest;
import com.sprint.mission.discodeit.dto.response.ChannelResponse;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.service.ChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * ========================================
 * ChannelService 인터페이스의 기본 구현체
 * ========================================
 *
 * 이 클래스는 채널 관련 비즈니스 로직을 구현합니다.
 * PUBLIC/PRIVATE 채널 생성, 조회, 수정, 삭제 기능을 제공합니다.
 *
 * [의존하는 Repository]
 * - ChannelRepository: 채널 데이터 저장/조회
 * - ReadStatusRepository: PRIVATE 채널 참여자 관리
 * - MessageRepository: 채널 삭제 시 메시지 삭제
 * - BinaryContentRepository: 메시지 첨부파일 삭제
 *
 * [어노테이션 설명]
 * @Service: Spring Bean으로 등록
 * @RequiredArgsConstructor: final 필드에 대한 생성자 자동 생성
 */
@Service
@RequiredArgsConstructor
public class BasicChannelService implements ChannelService {
    /**
     * 채널 정보를 저장하고 조회하는 리포지토리
     */
    private final ChannelRepository channelRepository;

    /**
     * 채널별 사용자 읽기 상태(참여자 정보)를 저장하고 조회하는 리포지토리
     */
    private final ReadStatusRepository readStatusRepository;

    /**
     * 메시지 정보를 저장하고 조회하는 리포지토리
     */
    private final MessageRepository messageRepository;

    /**
     * 바이너리 콘텐츠(첨부파일)를 저장하고 조회하는 리포지토리
     */
    private final BinaryContentRepository binaryContentRepository;

    /**
     * PUBLIC 채널을 생성합니다.
     *
     * PUBLIC 채널은 이름과 설명을 가지며, 모든 사용자가 접근 가능합니다.
     */
    @Override
    public ChannelResponse createPublic(PublicChannelCreateRequest request) {
        // Channel 엔티티 생성 (타입: PUBLIC)
        Channel channel = new Channel(ChannelType.PUBLIC, request.name(), request.description());
        // 저장
        Channel savedChannel = channelRepository.save(channel);
        // DTO로 변환하여 반환
        return toChannelResponse(savedChannel);
    }

    /**
     * ========================================
     * PRIVATE 채널을 생성합니다.
     * ========================================
     *
     * [처리 순서]
     * 1. PRIVATE 타입 채널 생성 (name, description은 null)
     * 2. 채널 저장
     * 3. 각 참여자에 대한 ReadStatus 생성 및 저장
     * 4. ChannelResponse 반환 (participantIds 포함)
     *
     * [ReadStatus의 역할]
     * PRIVATE 채널에서 ReadStatus는 두 가지 역할을 합니다:
     * 1. 참여자 관리: ReadStatus가 있는 사용자만 채널에 접근 가능
     * 2. 읽기 상태 추적: 안 읽은 메시지 수 계산
     */
    @Override
    public ChannelResponse createPrivate(PrivateChannelCreateRequest request) {
        // 1단계: PRIVATE 채널 생성 (이름, 설명 없음)
        Channel channel = new Channel(ChannelType.PRIVATE, null, null);
        Channel savedChannel = channelRepository.save(channel);

        // 2단계: 각 참여자에 대해 ReadStatus 생성
        // 이를 통해 참여자 목록이 관리됩니다.
        for (UUID userId : request.memberIds()) {
            ReadStatus readStatus = new ReadStatus(
                    userId,
                    savedChannel.getId(),
                    Instant.now()  // 생성 시점을 마지막 읽은 시간으로 설정
            );
            readStatusRepository.save(readStatus);
        }

        return toChannelResponse(savedChannel);
    }

    @Override
    public ChannelResponse find(UUID id) {
        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Channel not found: " + id));
        return toChannelResponse(channel);
    }

    /**
     * ========================================
     * 특정 사용자가 접근 가능한 모든 채널을 조회합니다.
     * ========================================
     *
     * [조회 로직]
     * 1. 모든 채널 조회
     * 2. 각 채널에 대해 접근 가능 여부 확인:
     *    - PUBLIC: 모든 사용자 접근 가능 → 포함
     *    - PRIVATE: ReadStatus가 있는 사용자만 → 해당 userId의 ReadStatus 확인
     * 3. 접근 가능한 채널만 필터링하여 반환
     */
    @Override
    public List<ChannelResponse> findAllByUserId(UUID userId) {
        // 모든 채널 조회
        List<Channel> allChannels = channelRepository.findAll();

        // 접근 가능한 채널만 필터링
        return allChannels.stream()
                .filter(channel -> {
                    if (channel.getType() == ChannelType.PUBLIC) {
                        // PUBLIC 채널은 모든 사용자가 접근 가능
                        return true;
                    } else {
                        // PRIVATE 채널은 ReadStatus가 있는 사용자만 접근 가능
                        // isPresent(): Optional에 값이 있으면 true
                        return readStatusRepository.findByUserIdAndChannelId(userId, channel.getId()).isPresent();
                    }
                })
                .map(this::toChannelResponse)  // 메서드 참조로 DTO 변환
                .toList();
    }

    /**
     * 채널 정보를 수정합니다.
     *
     * [주의] PUBLIC 채널만 수정 가능합니다!
     */
    @Override
    public ChannelResponse update(UUID id, ChannelUpdateRequest request) {
        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Channel not found: " + id));

        // PRIVATE 채널은 수정 불가
        // PRIVATE 채널은 이름/설명이 없으므로 수정할 것이 없습니다.
        if (channel.getType() == ChannelType.PRIVATE) {
            throw new IllegalArgumentException("Private channel cannot be updated");
        }

        // 채널 정보 업데이트 (null이 아닌 필드만)
        channel.update(request.name(), request.description());
        Channel savedChannel = channelRepository.save(channel);
        return toChannelResponse(savedChannel);
    }

    /**
     * ========================================
     * 채널을 삭제합니다. (Cascading Delete)
     * ========================================
     *
     * [삭제 순서 - 중요!]
     * 참조 무결성을 유지하기 위해 순서가 중요합니다.
     *
     * 1. 채널의 모든 메시지 조회
     * 2. 각 메시지의 첨부파일 삭제
     * 3. 채널의 모든 메시지 삭제
     * 4. 채널의 모든 ReadStatus 삭제
     * 5. 채널 삭제
     *
     * [왜 이 순서인가요?]
     * - 메시지가 있는데 채널을 먼저 삭제하면 고아 메시지가 됨
     * - 첨부파일이 있는데 메시지를 먼저 삭제하면 고아 파일이 됨
     */
    @Override
    public void delete(UUID id) {
        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Channel not found: " + id));

        // 1단계: 채널의 모든 메시지 조회
        List<Message> messages = messageRepository.findAllByChannelId(id);

        // 2단계: 각 메시지의 첨부파일 삭제
        for (Message message : messages) {
            if (message.getAttachmentIds() != null) {
                for (UUID attachmentId : message.getAttachmentIds()) {
                    binaryContentRepository.deleteById(attachmentId);
                }
            }
        }

        // 3단계: 채널의 모든 메시지 삭제
        messageRepository.deleteAllByChannelId(id);

        // 4단계: 채널의 모든 ReadStatus 삭제
        readStatusRepository.deleteAllByChannelId(id);

        // 5단계: 채널 삭제
        channelRepository.deleteById(id);
    }

    /**
     * ========================================
     * Channel 엔티티를 ChannelResponse DTO로 변환
     * ========================================
     *
     * [추가 정보 조회]
     * 1. lastMessageAt: 채널의 마지막 메시지 시간
     * 2. participantIds: PRIVATE 채널의 경우 참여자 목록
     */
    private ChannelResponse toChannelResponse(Channel channel) {
        // 최근 메시지 시간 조회
        // 채널의 모든 메시지 중 가장 최근 createdAt을 찾음
        Instant lastMessageAt = messageRepository.findAllByChannelId(channel.getId()).stream()
                .map(Message::getCreatedAt)
                .max(Instant::compareTo)  // 가장 큰(최신) 시간 찾기
                .orElse(null);            // 메시지가 없으면 null

        // PRIVATE 채널인 경우 참여자 ID 목록 조회
        List<UUID> participantIds = null;
        if (channel.getType() == ChannelType.PRIVATE) {
            participantIds = readStatusRepository.findAllByChannelId(channel.getId()).stream()
                    .map(ReadStatus::getUserId)
                    .toList();
        }

        return new ChannelResponse(
                channel.getId(),
                channel.getType(),
                channel.getName(),
                channel.getDescription(),
                participantIds,
                lastMessageAt,
                channel.getCreatedAt(),
                channel.getUpdatedAt()
        );
    }
}
