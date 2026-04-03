package com.sprint.mission.discodeit.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.discodeit.config.TestJpaConfig;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.entity.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import(TestJpaConfig.class)
@ActiveProfiles("test")
class ChannelRepositoryTest {

  @Autowired
  private ChannelRepository channelRepository;

  @Autowired
  private TestEntityManager em;

  @Test
  @DisplayName("유저별 채널 조회 - PUBLIC 채널 포함")
  void findAllByUserWithDetails_publicChannel() {
    // given
    Channel publicChannel = new Channel(ChannelType.PUBLIC, "general", "일반 채널");
    em.persistAndFlush(publicChannel);

    User user = new User("testuser", "test@test.com", "password", null);
    em.persistAndFlush(user);

    // when
    List<Channel> result = channelRepository.findAllByUserWithDetails(ChannelType.PUBLIC,
        user.getId());

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("general");
  }

  @Test
  @DisplayName("유저별 채널 조회 - PRIVATE 채널 (ReadStatus로 참여)")
  void findAllByUserWithDetails_privateChannel() {
    // given
    User user = new User("testuser", "test@test.com", "password", null);
    em.persistAndFlush(user);

    Channel privateChannel = new Channel(ChannelType.PRIVATE, null, null);
    em.persistAndFlush(privateChannel);

    ReadStatus readStatus = new ReadStatus(user, privateChannel, Instant.now());
    em.persistAndFlush(readStatus);

    // when
    List<Channel> result = channelRepository.findAllByUserWithDetails(ChannelType.PUBLIC,
        user.getId());

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getType()).isEqualTo(ChannelType.PRIVATE);
  }

  @Test
  @DisplayName("유저별 채널 조회 - 참여하지 않은 PRIVATE 채널 제외")
  void findAllByUserWithDetails_excludeNonParticipant() {
    // given
    User user = new User("testuser", "test@test.com", "password", null);
    em.persistAndFlush(user);

    Channel privateChannel = new Channel(ChannelType.PRIVATE, null, null);
    em.persistAndFlush(privateChannel);
    // ReadStatus 없음 = 참여하지 않음

    // when
    List<Channel> result = channelRepository.findAllByUserWithDetails(ChannelType.PUBLIC,
        user.getId());

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("유저별 채널 조회 - 빈 결과")
  void findAllByUserWithDetails_empty() {
    // when
    List<Channel> result = channelRepository.findAllByUserWithDetails(ChannelType.PUBLIC,
        UUID.randomUUID());

    // then
    assertThat(result).isEmpty();
  }
}
