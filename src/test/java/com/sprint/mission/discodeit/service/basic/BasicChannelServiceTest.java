package com.sprint.mission.discodeit.service.basic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.sprint.mission.discodeit.dto.data.ChannelDto;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.request.PrivateChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.PublicChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.PublicChannelUpdateRequest;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.mapper.ChannelMapper;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BasicChannelServiceTest {

  @InjectMocks
  private BasicChannelService channelService;

  @Mock
  private ChannelRepository channelRepository;
  @Mock
  private ReadStatusRepository readStatusRepository;
  @Mock
  private MessageRepository messageRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private BinaryContentRepository binaryContentRepository;
  @Mock
  private BinaryContentStorage binaryContentStorage;
  @Mock
  private ChannelMapper channelMapper;
  @Mock
  private UserMapper userMapper;

  @Test
  @DisplayName("공개 채널 생성 성공")
  void createPublic_success() {
    // given
    PublicChannelCreateRequest request = new PublicChannelCreateRequest("general", "일반 채널");
    Channel channel = new Channel(ChannelType.PUBLIC, "general", "일반 채널");
    UUID channelId = UUID.randomUUID();
    ReflectionTestUtils.setField(channel, "id", channelId);

    ChannelDto expectedDto = new ChannelDto(channelId, ChannelType.PUBLIC, "general", "일반 채널",
        List.of(), null);

    given(channelRepository.save(any(Channel.class))).willReturn(channel);
    given(messageRepository.findLastCreatedAtByChannelId(channelId)).willReturn(Optional.empty());
    given(channelMapper.toDto(eq(channel), eq(List.of()), eq(null))).willReturn(expectedDto);

    // when
    ChannelDto result = channelService.create(request);

    // then
    assertThat(result.name()).isEqualTo("general");
    assertThat(result.type()).isEqualTo(ChannelType.PUBLIC);
    then(channelRepository).should().save(any(Channel.class));
  }

  @Test
  @DisplayName("비공개 채널 생성 성공")
  void createPrivate_success() {
    // given
    UUID userId1 = UUID.randomUUID();
    UUID userId2 = UUID.randomUUID();
    PrivateChannelCreateRequest request = new PrivateChannelCreateRequest(
        List.of(userId1, userId2));

    Channel channel = new Channel(ChannelType.PRIVATE, null, null);
    UUID channelId = UUID.randomUUID();
    ReflectionTestUtils.setField(channel, "id", channelId);

    User user1 = new User("user1", "u1@test.com", "pw", null);
    User user2 = new User("user2", "u2@test.com", "pw", null);
    ReflectionTestUtils.setField(user1, "id", userId1);
    ReflectionTestUtils.setField(user2, "id", userId2);

    UserDto userDto1 = new UserDto(userId1, "user1", "u1@test.com", null, true);
    UserDto userDto2 = new UserDto(userId2, "user2", "u2@test.com", null, true);

    ChannelDto expectedDto = new ChannelDto(channelId, ChannelType.PRIVATE, null, null,
        List.of(userDto1, userDto2), null);

    given(channelRepository.save(any(Channel.class))).willReturn(channel);
    given(userRepository.findById(userId1)).willReturn(Optional.of(user1));
    given(userRepository.findById(userId2)).willReturn(Optional.of(user2));
    given(readStatusRepository.save(any(ReadStatus.class))).willReturn(null);
    given(userMapper.toDto(user1)).willReturn(userDto1);
    given(userMapper.toDto(user2)).willReturn(userDto2);
    given(channelMapper.toDto(eq(channel), any(), eq(null))).willReturn(expectedDto);

    // when
    ChannelDto result = channelService.create(request);

    // then
    assertThat(result.type()).isEqualTo(ChannelType.PRIVATE);
    assertThat(result.participants()).hasSize(2);
  }

  @Test
  @DisplayName("비공개 채널 생성 실패 - 참여자 없음")
  void createPrivate_fail_userNotFound() {
    // given
    UUID userId = UUID.randomUUID();
    PrivateChannelCreateRequest request = new PrivateChannelCreateRequest(List.of(userId));

    Channel channel = new Channel(ChannelType.PRIVATE, null, null);
    UUID channelId = UUID.randomUUID();
    ReflectionTestUtils.setField(channel, "id", channelId);

    given(channelRepository.save(any(Channel.class))).willReturn(channel);
    given(userRepository.findById(userId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> channelService.create(request))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  @DisplayName("채널 수정 성공")
  void update_success() {
    // given
    UUID channelId = UUID.randomUUID();
    Channel channel = new Channel(ChannelType.PUBLIC, "old", "old desc");
    ReflectionTestUtils.setField(channel, "id", channelId);

    PublicChannelUpdateRequest request = new PublicChannelUpdateRequest("new", "new desc");
    ChannelDto expectedDto = new ChannelDto(channelId, ChannelType.PUBLIC, "new", "new desc",
        List.of(), null);

    given(channelRepository.findById(channelId)).willReturn(Optional.of(channel));
    given(messageRepository.findLastCreatedAtByChannelId(channelId)).willReturn(Optional.empty());
    given(channelMapper.toDto(eq(channel), eq(List.of()), eq(null))).willReturn(expectedDto);

    // when
    ChannelDto result = channelService.update(channelId, request);

    // then
    assertThat(result.name()).isEqualTo("new");
    assertThat(result.description()).isEqualTo("new desc");
  }

  @Test
  @DisplayName("채널 수정 실패 - 채널 없음")
  void update_fail_notFound() {
    // given
    UUID channelId = UUID.randomUUID();
    PublicChannelUpdateRequest request = new PublicChannelUpdateRequest("new", "new desc");
    given(channelRepository.findById(channelId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> channelService.update(channelId, request))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  @DisplayName("채널 수정 실패 - PRIVATE 채널")
  void update_fail_privateChannel() {
    // given
    UUID channelId = UUID.randomUUID();
    Channel channel = new Channel(ChannelType.PRIVATE, null, null);
    ReflectionTestUtils.setField(channel, "id", channelId);

    PublicChannelUpdateRequest request = new PublicChannelUpdateRequest("new", "new desc");
    given(channelRepository.findById(channelId)).willReturn(Optional.of(channel));

    // when & then
    assertThatThrownBy(() -> channelService.update(channelId, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Private channel");
  }

  @Test
  @DisplayName("채널 삭제 성공")
  void delete_success() {
    // given
    UUID channelId = UUID.randomUUID();
    Channel channel = new Channel(ChannelType.PUBLIC, "general", "desc");
    ReflectionTestUtils.setField(channel, "id", channelId);

    given(channelRepository.findById(channelId)).willReturn(Optional.of(channel));
    given(messageRepository.findAllByChannel_Id(channelId)).willReturn(List.of());

    // when
    channelService.delete(channelId);

    // then
    then(channelRepository).should().delete(channel);
  }

  @Test
  @DisplayName("채널 삭제 성공 - 첨부파일 포함 메시지")
  void delete_success_withAttachments() {
    // given
    UUID channelId = UUID.randomUUID();
    Channel channel = new Channel(ChannelType.PUBLIC, "general", "desc");
    ReflectionTestUtils.setField(channel, "id", channelId);

    BinaryContent attachment = new BinaryContent("file.txt", 100L, "text/plain");
    UUID attachmentId = UUID.randomUUID();
    ReflectionTestUtils.setField(attachment, "id", attachmentId);

    User author = new User("user", "u@test.com", "pw", null);
    Message message = new Message("hello", channel, author, List.of(attachment));

    given(channelRepository.findById(channelId)).willReturn(Optional.of(channel));
    given(messageRepository.findAllByChannel_Id(channelId)).willReturn(List.of(message));

    // when
    channelService.delete(channelId);

    // then
    then(channelRepository).should().delete(channel);
    then(binaryContentStorage).should().delete(attachmentId);
    then(binaryContentRepository).should().deleteById(attachmentId);
  }

  @Test
  @DisplayName("채널 삭제 실패 - 채널 없음")
  void delete_fail_notFound() {
    // given
    UUID channelId = UUID.randomUUID();
    given(channelRepository.findById(channelId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> channelService.delete(channelId))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  @DisplayName("유저별 채널 조회 성공")
  void findAllByUserId_success() {
    // given
    UUID userId = UUID.randomUUID();
    Channel channel = new Channel(ChannelType.PUBLIC, "general", "desc");
    UUID channelId = UUID.randomUUID();
    ReflectionTestUtils.setField(channel, "id", channelId);

    ChannelDto expectedDto = new ChannelDto(channelId, ChannelType.PUBLIC, "general", "desc",
        List.of(), null);

    given(channelRepository.findAllByUserWithDetails(ChannelType.PUBLIC, userId))
        .willReturn(List.of(channel));
    given(messageRepository.findLastCreatedAtByChannelIds(List.of(channelId)))
        .willReturn(List.of());
    given(channelMapper.toDto(eq(channel), eq(List.of()), eq(null))).willReturn(expectedDto);

    // when
    List<ChannelDto> result = channelService.findAllByUserId(userId);

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).name()).isEqualTo("general");
  }

  @Test
  @DisplayName("유저별 채널 조회 성공 - PRIVATE 채널 참여자 포함")
  void findAllByUserId_success_withPrivateChannel() {
    // given
    UUID userId = UUID.randomUUID();
    UUID participantId = UUID.randomUUID();

    Channel privateChannel = new Channel(ChannelType.PRIVATE, null, null);
    UUID channelId = UUID.randomUUID();
    ReflectionTestUtils.setField(privateChannel, "id", channelId);

    User participant = new User("participant", "p@test.com", "pw", null);
    ReflectionTestUtils.setField(participant, "id", participantId);

    ReadStatus rs = new ReadStatus(participant, privateChannel, Instant.now());
    // Channel의 readStatuses 필드에 직접 추가
    ReflectionTestUtils.setField(privateChannel, "readStatuses", List.of(rs));

    UserDto participantDto = new UserDto(participantId, "participant", "p@test.com", null, true);
    ChannelDto expectedDto = new ChannelDto(channelId, ChannelType.PRIVATE, null, null,
        List.of(participantDto), null);

    given(channelRepository.findAllByUserWithDetails(ChannelType.PUBLIC, userId))
        .willReturn(List.of(privateChannel));
    given(messageRepository.findLastCreatedAtByChannelIds(List.of(channelId)))
        .willReturn(List.of());
    given(userMapper.toDto(participant)).willReturn(participantDto);
    given(channelMapper.toDto(eq(privateChannel), any(), eq(null))).willReturn(expectedDto);

    // when
    List<ChannelDto> result = channelService.findAllByUserId(userId);

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).participants()).hasSize(1);
  }

  @Test
  @DisplayName("유저별 채널 조회 성공 - lastMessageAt 포함")
  void findAllByUserId_success_withLastMessageAt() {
    // given
    UUID userId = UUID.randomUUID();
    Channel channel = new Channel(ChannelType.PUBLIC, "general", "desc");
    UUID channelId = UUID.randomUUID();
    ReflectionTestUtils.setField(channel, "id", channelId);

    Instant lastMessageAt = Instant.now();
    ChannelDto expectedDto = new ChannelDto(channelId, ChannelType.PUBLIC, "general", "desc",
        List.of(), lastMessageAt);

    given(channelRepository.findAllByUserWithDetails(ChannelType.PUBLIC, userId))
        .willReturn(List.of(channel));
    Object[] row = new Object[]{channelId, lastMessageAt};
    List<Object[]> lastMessageAtResult = new java.util.ArrayList<>();
    lastMessageAtResult.add(row);
    given(messageRepository.findLastCreatedAtByChannelIds(List.of(channelId)))
        .willReturn(lastMessageAtResult);
    given(channelMapper.toDto(eq(channel), eq(List.of()), eq(lastMessageAt)))
        .willReturn(expectedDto);

    // when
    List<ChannelDto> result = channelService.findAllByUserId(userId);

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).lastMessageAt()).isEqualTo(lastMessageAt);
  }
}
