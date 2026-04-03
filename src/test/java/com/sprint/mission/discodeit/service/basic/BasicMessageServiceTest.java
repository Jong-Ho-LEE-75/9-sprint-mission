package com.sprint.mission.discodeit.service.basic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.sprint.mission.discodeit.dto.data.MessageDto;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageUpdateRequest;
import com.sprint.mission.discodeit.dto.response.PageResponse;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.mapper.MessageMapper;
import com.sprint.mission.discodeit.mapper.PageResponseMapper;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BasicMessageServiceTest {

  @InjectMocks
  private BasicMessageService messageService;

  @Mock
  private MessageRepository messageRepository;
  @Mock
  private ChannelRepository channelRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private BinaryContentRepository binaryContentRepository;
  @Mock
  private BinaryContentStorage binaryContentStorage;
  @Mock
  private MessageMapper messageMapper;
  @Mock
  private PageResponseMapper pageResponseMapper;

  @Test
  @DisplayName("메시지 생성 성공")
  void create_success() {
    // given
    UUID channelId = UUID.randomUUID();
    UUID authorId = UUID.randomUUID();
    MessageCreateRequest request = new MessageCreateRequest("hello", channelId, authorId);

    Channel channel = new Channel(ChannelType.PUBLIC, "general", null);
    ReflectionTestUtils.setField(channel, "id", channelId);
    User author = new User("user1", "u1@test.com", "pw", null);
    ReflectionTestUtils.setField(author, "id", authorId);

    Message message = new Message("hello", channel, author, List.of());
    UUID messageId = UUID.randomUUID();
    ReflectionTestUtils.setField(message, "id", messageId);

    Instant now = Instant.now();
    MessageDto expectedDto = new MessageDto(messageId, now, now, "hello", channelId,
        new UserDto(authorId, "user1", "u1@test.com", null, true), List.of());

    given(channelRepository.findById(channelId)).willReturn(Optional.of(channel));
    given(userRepository.findById(authorId)).willReturn(Optional.of(author));
    given(messageRepository.save(any(Message.class))).willReturn(message);
    given(messageMapper.toDto(any(Message.class))).willReturn(expectedDto);

    // when
    MessageDto result = messageService.create(request, List.of());

    // then
    assertThat(result.content()).isEqualTo("hello");
    assertThat(result.channelId()).isEqualTo(channelId);
    then(messageRepository).should().save(any(Message.class));
  }

  @Test
  @DisplayName("메시지 생성 성공 - 첨부파일 포함")
  void create_success_withAttachments() {
    // given
    UUID channelId = UUID.randomUUID();
    UUID authorId = UUID.randomUUID();
    MessageCreateRequest request = new MessageCreateRequest("hello", channelId, authorId);
    BinaryContentCreateRequest attachmentReq = new BinaryContentCreateRequest("file.txt",
        "text/plain", new byte[]{1, 2});

    Channel channel = new Channel(ChannelType.PUBLIC, "general", null);
    ReflectionTestUtils.setField(channel, "id", channelId);
    User author = new User("user1", "u1@test.com", "pw", null);
    ReflectionTestUtils.setField(author, "id", authorId);

    BinaryContent attachment = new BinaryContent("file.txt", 2L, "text/plain");
    UUID attachmentId = UUID.randomUUID();
    ReflectionTestUtils.setField(attachment, "id", attachmentId);

    Message message = new Message("hello", channel, author, List.of(attachment));
    UUID messageId = UUID.randomUUID();
    ReflectionTestUtils.setField(message, "id", messageId);

    MessageDto expectedDto = new MessageDto(messageId, Instant.now(), Instant.now(), "hello",
        channelId, null, List.of());

    given(channelRepository.findById(channelId)).willReturn(Optional.of(channel));
    given(userRepository.findById(authorId)).willReturn(Optional.of(author));
    given(binaryContentRepository.save(any(BinaryContent.class))).willReturn(attachment);
    given(binaryContentStorage.put(any(UUID.class), any(byte[].class))).willReturn(attachmentId);
    given(messageRepository.save(any(Message.class))).willReturn(message);
    given(messageMapper.toDto(any(Message.class))).willReturn(expectedDto);

    // when
    MessageDto result = messageService.create(request, List.of(attachmentReq));

    // then
    assertThat(result).isNotNull();
    then(binaryContentRepository).should().save(any(BinaryContent.class));
    then(binaryContentStorage).should().put(any(UUID.class), any(byte[].class));
  }

  @Test
  @DisplayName("메시지 생성 실패 - 채널 없음")
  void create_fail_channelNotFound() {
    // given
    UUID channelId = UUID.randomUUID();
    UUID authorId = UUID.randomUUID();
    MessageCreateRequest request = new MessageCreateRequest("hello", channelId, authorId);

    given(channelRepository.findById(channelId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> messageService.create(request, List.of()))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessageContaining("Channel");
  }

  @Test
  @DisplayName("메시지 생성 실패 - 작성자 없음")
  void create_fail_authorNotFound() {
    // given
    UUID channelId = UUID.randomUUID();
    UUID authorId = UUID.randomUUID();
    MessageCreateRequest request = new MessageCreateRequest("hello", channelId, authorId);

    Channel channel = new Channel(ChannelType.PUBLIC, "general", null);
    given(channelRepository.findById(channelId)).willReturn(Optional.of(channel));
    given(userRepository.findById(authorId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> messageService.create(request, List.of()))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessageContaining("Author");
  }

  @Test
  @DisplayName("메시지 수정 성공")
  void update_success() {
    // given
    UUID messageId = UUID.randomUUID();
    Channel channel = new Channel(ChannelType.PUBLIC, "general", null);
    User author = new User("user1", "u@test.com", "pw", null);
    Message message = new Message("old content", channel, author, List.of());
    ReflectionTestUtils.setField(message, "id", messageId);

    MessageUpdateRequest request = new MessageUpdateRequest("new content");
    MessageDto expectedDto = new MessageDto(messageId, Instant.now(), Instant.now(), "new content",
        UUID.randomUUID(), null, List.of());

    given(messageRepository.findById(messageId)).willReturn(Optional.of(message));
    given(messageMapper.toDto(any(Message.class))).willReturn(expectedDto);

    // when
    MessageDto result = messageService.update(messageId, request);

    // then
    assertThat(result.content()).isEqualTo("new content");
  }

  @Test
  @DisplayName("메시지 수정 실패 - 메시지 없음")
  void update_fail_notFound() {
    // given
    UUID messageId = UUID.randomUUID();
    MessageUpdateRequest request = new MessageUpdateRequest("new content");
    given(messageRepository.findById(messageId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> messageService.update(messageId, request))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  @DisplayName("메시지 삭제 성공")
  void delete_success() {
    // given
    UUID messageId = UUID.randomUUID();
    Channel channel = new Channel(ChannelType.PUBLIC, "general", null);
    User author = new User("user1", "u@test.com", "pw", null);
    Message message = new Message("hello", channel, author, List.of());
    ReflectionTestUtils.setField(message, "id", messageId);

    given(messageRepository.findById(messageId)).willReturn(Optional.of(message));

    // when
    messageService.delete(messageId);

    // then
    then(messageRepository).should().delete(message);
  }

  @Test
  @DisplayName("메시지 삭제 성공 - 첨부파일 포함")
  void delete_success_withAttachments() {
    // given
    UUID messageId = UUID.randomUUID();
    BinaryContent attachment = new BinaryContent("file.txt", 100L, "text/plain");
    UUID attachmentId = UUID.randomUUID();
    ReflectionTestUtils.setField(attachment, "id", attachmentId);

    Channel channel = new Channel(ChannelType.PUBLIC, "general", null);
    User author = new User("user1", "u@test.com", "pw", null);
    Message message = new Message("hello", channel, author, List.of(attachment));
    ReflectionTestUtils.setField(message, "id", messageId);

    given(messageRepository.findById(messageId)).willReturn(Optional.of(message));

    // when
    messageService.delete(messageId);

    // then
    then(messageRepository).should().delete(message);
    then(binaryContentStorage).should().delete(attachmentId);
    then(binaryContentRepository).should().deleteById(attachmentId);
  }

  @Test
  @DisplayName("메시지 삭제 실패 - 메시지 없음")
  void delete_fail_notFound() {
    // given
    UUID messageId = UUID.randomUUID();
    given(messageRepository.findById(messageId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> messageService.delete(messageId))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  @DisplayName("채널별 메시지 조회 성공 - 커서 없음")
  void findAllByChannelId_success_noCursor() {
    // given
    UUID channelId = UUID.randomUUID();
    PageRequest pageRequest = PageRequest.of(0, 50);
    SliceImpl<Message> slice = new SliceImpl<>(List.of(), pageRequest, false);
    PageResponse<MessageDto> expectedResponse = new PageResponse<>(List.of(), null, 50, false,
        null);

    given(messageRepository.findAllByChannelId(eq(channelId), any(PageRequest.class)))
        .willReturn(slice);
    given(pageResponseMapper.fromSlice(any(), any())).willReturn((PageResponse) expectedResponse);

    // when
    PageResponse<MessageDto> result = messageService.findAllByChannelId(channelId, null, 50);

    // then
    assertThat(result.hasNext()).isFalse();
    then(messageRepository).should().findAllByChannelId(eq(channelId), any(PageRequest.class));
  }

  @Test
  @DisplayName("채널별 메시지 조회 성공 - 커서 있음")
  void findAllByChannelId_success_withCursor() {
    // given
    UUID channelId = UUID.randomUUID();
    Instant cursor = Instant.now();
    PageRequest pageRequest = PageRequest.of(0, 50);
    SliceImpl<Message> slice = new SliceImpl<>(List.of(), pageRequest, false);
    PageResponse<MessageDto> expectedResponse = new PageResponse<>(List.of(), null, 50, false,
        null);

    given(messageRepository.findAllByChannelIdAndCreatedAtBefore(eq(channelId), eq(cursor),
        any(PageRequest.class)))
        .willReturn(slice);
    given(pageResponseMapper.fromSlice(any(), any())).willReturn((PageResponse) expectedResponse);

    // when
    PageResponse<MessageDto> result = messageService.findAllByChannelId(channelId, cursor, 50);

    // then
    assertThat(result).isNotNull();
    then(messageRepository).should()
        .findAllByChannelIdAndCreatedAtBefore(eq(channelId), eq(cursor), any(PageRequest.class));
  }

  @Test
  @DisplayName("채널별 메시지 조회 - size가 0 이하일 때 기본값 50 사용")
  void findAllByChannelId_defaultSize() {
    // given
    UUID channelId = UUID.randomUUID();
    SliceImpl<Message> slice = new SliceImpl<>(List.of(), PageRequest.of(0, 50), false);
    PageResponse<MessageDto> expectedResponse = new PageResponse<>(List.of(), null, 50, false,
        null);

    given(messageRepository.findAllByChannelId(eq(channelId), any(PageRequest.class)))
        .willReturn(slice);
    given(pageResponseMapper.fromSlice(any(), any())).willReturn((PageResponse) expectedResponse);

    // when
    PageResponse<MessageDto> result = messageService.findAllByChannelId(channelId, null, 0);

    // then
    assertThat(result.size()).isEqualTo(50);
  }
}
