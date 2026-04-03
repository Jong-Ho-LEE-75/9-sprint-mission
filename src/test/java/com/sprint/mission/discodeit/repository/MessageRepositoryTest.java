package com.sprint.mission.discodeit.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.discodeit.config.TestJpaConfig;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import(TestJpaConfig.class)
@ActiveProfiles("test")
class MessageRepositoryTest {

  @Autowired
  private MessageRepository messageRepository;

  @Autowired
  private TestEntityManager em;

  private User author;
  private Channel channel;

  @BeforeEach
  void setUp() {
    author = new User("testuser", "test@test.com", "password", null);
    em.persistAndFlush(author);
    channel = new Channel(ChannelType.PUBLIC, "general", "일반 채널");
    em.persistAndFlush(channel);
  }

  @Test
  @DisplayName("채널별 메시지 조회 성공 - 커서 없음")
  void findAllByChannelId_success() {
    // given
    Message msg1 = new Message("hello", channel, author, List.of());
    Message msg2 = new Message("world", channel, author, List.of());
    em.persistAndFlush(msg1);
    em.persistAndFlush(msg2);

    // when
    Slice<Message> result = messageRepository.findAllByChannelId(channel.getId(),
        PageRequest.of(0, 10));

    // then
    assertThat(result.getContent()).hasSize(2);
  }

  @Test
  @DisplayName("채널별 메시지 조회 - 빈 결과")
  void findAllByChannelId_empty() {
    // when
    Slice<Message> result = messageRepository.findAllByChannelId(channel.getId(),
        PageRequest.of(0, 10));

    // then
    assertThat(result.getContent()).isEmpty();
  }

  @Test
  @DisplayName("채널별 메시지 조회 - 커서 기반 페이지네이션")
  void findAllByChannelIdAndCreatedAtBefore_success() {
    // given
    Message msg = new Message("hello", channel, author, List.of());
    em.persistAndFlush(msg);

    // when - 미래 시간을 커서로 사용하면 모든 메시지가 조회됨
    Slice<Message> result = messageRepository.findAllByChannelIdAndCreatedAtBefore(
        channel.getId(), Instant.now().plusSeconds(60), PageRequest.of(0, 10));

    // then
    assertThat(result.getContent()).hasSize(1);
  }

  @Test
  @DisplayName("채널별 메시지 조회 - 과거 커서로 빈 결과")
  void findAllByChannelIdAndCreatedAtBefore_empty() {
    // given
    Message msg = new Message("hello", channel, author, List.of());
    em.persistAndFlush(msg);

    // when - 매우 과거 시간을 커서로 사용
    Slice<Message> result = messageRepository.findAllByChannelIdAndCreatedAtBefore(
        channel.getId(), Instant.parse("2000-01-01T00:00:00Z"), PageRequest.of(0, 10));

    // then
    assertThat(result.getContent()).isEmpty();
  }

  @Test
  @DisplayName("채널의 마지막 메시지 시간 조회 성공")
  void findLastCreatedAtByChannelId_success() {
    // given
    Message msg = new Message("hello", channel, author, List.of());
    em.persistAndFlush(msg);

    // when
    Optional<Instant> result = messageRepository.findLastCreatedAtByChannelId(channel.getId());

    // then
    assertThat(result).isPresent();
  }

  @Test
  @DisplayName("채널의 마지막 메시지 시간 조회 - 메시지 없음")
  void findLastCreatedAtByChannelId_empty() {
    // when
    Optional<Instant> result = messageRepository.findLastCreatedAtByChannelId(channel.getId());

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("여러 채널의 마지막 메시지 시간 일괄 조회")
  void findLastCreatedAtByChannelIds_success() {
    // given
    Message msg = new Message("hello", channel, author, List.of());
    em.persistAndFlush(msg);

    Channel channel2 = new Channel(ChannelType.PUBLIC, "random", null);
    em.persistAndFlush(channel2);

    // when
    List<Object[]> result = messageRepository.findLastCreatedAtByChannelIds(
        List.of(channel.getId(), channel2.getId()));

    // then
    assertThat(result).hasSize(1); // channel2에는 메시지가 없으므로 1개만
    assertThat(result.get(0)[0]).isEqualTo(channel.getId());
  }

  @Test
  @DisplayName("채널별 메시지 목록 조회 (findAllByChannel_Id)")
  void findAllByChannel_Id_success() {
    // given
    Message msg = new Message("hello", channel, author, List.of());
    em.persistAndFlush(msg);

    // when
    List<Message> result = messageRepository.findAllByChannel_Id(channel.getId());

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getContent()).isEqualTo("hello");
  }
}
