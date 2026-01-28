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

@Service
@RequiredArgsConstructor
public class BasicChannelService implements ChannelService {
    private final ChannelRepository channelRepository;
    private final ReadStatusRepository readStatusRepository;
    private final MessageRepository messageRepository;
    private final BinaryContentRepository binaryContentRepository;

    @Override
    public ChannelResponse createPublic(PublicChannelCreateRequest request) {
        Channel channel = new Channel(ChannelType.PUBLIC, request.name(), request.description());
        Channel savedChannel = channelRepository.save(channel);
        return toChannelResponse(savedChannel);
    }

    @Override
    public ChannelResponse createPrivate(PrivateChannelCreateRequest request) {
        // PRIVATE 채널은 name, description 없음
        Channel channel = new Channel(ChannelType.PRIVATE, null, null);
        Channel savedChannel = channelRepository.save(channel);

        // 참여자별 ReadStatus 생성
        for (UUID userId : request.participantIds()) {
            ReadStatus readStatus = new ReadStatus(userId, savedChannel.getId(), Instant.now());
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

    @Override
    public List<ChannelResponse> findAllByUserId(UUID userId) {
        List<Channel> allChannels = channelRepository.findAll();

        return allChannels.stream()
                .filter(channel -> {
                    if (channel.getType() == ChannelType.PUBLIC) {
                        // PUBLIC 채널은 모두 조회 가능
                        return true;
                    } else {
                        // PRIVATE 채널은 참여한 User만 조회 가능
                        return readStatusRepository.findByUserIdAndChannelId(userId, channel.getId()).isPresent();
                    }
                })
                .map(this::toChannelResponse)
                .toList();
    }

    @Override
    public ChannelResponse update(UUID id, ChannelUpdateRequest request) {
        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Channel not found: " + id));

        // PRIVATE 채널은 수정 불가
        if (channel.getType() == ChannelType.PRIVATE) {
            throw new IllegalArgumentException("Private channel cannot be updated");
        }

        channel.update(request.name(), request.description());
        Channel savedChannel = channelRepository.save(channel);
        return toChannelResponse(savedChannel);
    }

    @Override
    public void delete(UUID id) {
        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Channel not found: " + id));

        // 관련 Message의 첨부파일 삭제 후 Message 삭제
        List<Message> messages = messageRepository.findAllByChannelId(id);
        for (Message message : messages) {
            if (message.getAttachmentIds() != null) {
                for (UUID attachmentId : message.getAttachmentIds()) {
                    binaryContentRepository.deleteById(attachmentId);
                }
            }
        }
        messageRepository.deleteAllByChannelId(id);

        // 관련 ReadStatus 삭제
        readStatusRepository.deleteAllByChannelId(id);
        // Channel 삭제
        channelRepository.deleteById(id);
    }

    private ChannelResponse toChannelResponse(Channel channel) {
        // 최근 메시지 시간 조회
        Instant lastMessageAt = messageRepository.findAllByChannelId(channel.getId()).stream()
                .map(Message::getCreatedAt)
                .max(Instant::compareTo)
                .orElse(null);

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