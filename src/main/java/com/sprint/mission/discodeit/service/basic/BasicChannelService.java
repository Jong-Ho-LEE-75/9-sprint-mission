package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.data.ChannelDto;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.request.PrivateChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.PublicChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.PublicChannelUpdateRequest;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.mapper.ChannelMapper;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.ChannelService;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class BasicChannelService implements ChannelService {

  private final ChannelRepository channelRepository;
  private final ReadStatusRepository readStatusRepository;
  private final MessageRepository messageRepository;
  private final UserRepository userRepository;
  private final BinaryContentRepository binaryContentRepository;
  private final BinaryContentStorage binaryContentStorage;
  private final ChannelMapper channelMapper;
  private final UserMapper userMapper;

  @Override
  @Transactional
  public ChannelDto create(PublicChannelCreateRequest request) {
    Channel channel = new Channel(ChannelType.PUBLIC, request.name(), request.description());
    channel = channelRepository.save(channel);
    Instant lastMessageAt = messageRepository.findLastCreatedAtByChannelId(channel.getId())
        .orElse(null);
    return channelMapper.toDto(channel, List.of(), lastMessageAt);
  }

  @Override
  @Transactional
  public ChannelDto create(PrivateChannelCreateRequest request) {
    Channel channel = new Channel(ChannelType.PRIVATE, null, null);
    channel = channelRepository.save(channel);

    Channel finalChannel = channel;
    List<User> participants = request.participantIds().stream()
        .map(userId -> userRepository.findById(userId)
            .orElseThrow(
                () -> new NoSuchElementException("User with id " + userId + " not found")))
        .toList();

    participants.stream()
        .map(user -> new ReadStatus(user, finalChannel, Instant.now()))
        .forEach(readStatusRepository::save);

    List<UserDto> participantDtos = participants.stream()
        .map(userMapper::toDto)
        .toList();

    return channelMapper.toDto(channel, participantDtos, null);
  }

  @Override
  @Transactional(readOnly = true)
  public ChannelDto find(UUID channelId) {
    Channel channel = channelRepository.findById(channelId)
        .orElseThrow(
            () -> new NoSuchElementException("Channel with id " + channelId + " not found"));
    Instant lastMessageAt = messageRepository.findLastCreatedAtByChannelId(channelId).orElse(null);
    List<UserDto> participants = List.of();
    if (channel.getType() == ChannelType.PRIVATE) {
      participants = readStatusRepository.findAllByChannel_Id(channelId).stream()
          .map(rs -> userMapper.toDto(rs.getUser()))
          .toList();
    }
    return channelMapper.toDto(channel, participants, lastMessageAt);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ChannelDto> findAllByUserId(UUID userId) {
    List<Channel> channels = channelRepository.findAllByUserWithDetails(ChannelType.PUBLIC, userId);
    List<UUID> channelIds = channels.stream().map(Channel::getId).toList();

    // lastMessageAt을 한 번의 쿼리로 일괄 조회 (N+1 방지)
    Map<UUID, Instant> lastMessageAtMap = messageRepository
        .findLastCreatedAtByChannelIds(channelIds)
        .stream()
        .collect(Collectors.toMap(
            row -> (UUID) row[0],
            row -> (Instant) row[1]
        ));

    return channels.stream()
        .map(channel -> {
          Instant lastMessageAt = lastMessageAtMap.get(channel.getId());
          // readStatuses와 user는 이미 JOIN FETCH로 로드됨 (N+1 없음)
          List<UserDto> participants = channel.getType() == ChannelType.PRIVATE
              ? channel.getReadStatuses().stream()
                  .map(rs -> userMapper.toDto(rs.getUser()))
                  .toList()
              : List.of();
          return channelMapper.toDto(channel, participants, lastMessageAt);
        })
        .toList();
  }

  @Override
  @Transactional
  public ChannelDto update(UUID channelId, PublicChannelUpdateRequest request) {
    Channel channel = channelRepository.findById(channelId)
        .orElseThrow(
            () -> new NoSuchElementException("Channel with id " + channelId + " not found"));
    if (channel.getType() == ChannelType.PRIVATE) {
      throw new IllegalArgumentException("Private channel cannot be updated");
    }
    channel.update(request.newName(), request.newDescription());
    Instant lastMessageAt = messageRepository.findLastCreatedAtByChannelId(channelId).orElse(null);
    return channelMapper.toDto(channel, List.of(), lastMessageAt);
  }

  @Override
  @Transactional
  public void delete(UUID channelId) {
    Channel channel = channelRepository.findById(channelId)
        .orElseThrow(
            () -> new NoSuchElementException("Channel with id " + channelId + " not found"));

    // 메시지 첨부파일 스토리지 정리
    List<BinaryContent> attachments = messageRepository.findAllByChannel_Id(channelId).stream()
        .flatMap(m -> m.getAttachments().stream())
        .toList();
    List<UUID> attachmentIds = attachments.stream().map(BinaryContent::getId).toList();

    // DB cascade: channel 삭제 시 messages, read_statuses, message_attachments 삭제
    channelRepository.delete(channel);

    // BinaryContent 엔티티 및 스토리지 정리
    attachmentIds.forEach(id -> {
      binaryContentStorage.delete(id);
      binaryContentRepository.deleteById(id);
    });
  }
}
