package com.sprint.mission.discodeit.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.discodeit.config.TestJpaConfig;
import com.sprint.mission.discodeit.entity.User;
import java.util.List;
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
class UserRepositoryTest {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private TestEntityManager em;

  @Test
  @DisplayName("이메일 존재 여부 확인 - 존재")
  void existsByEmail_true() {
    // given
    User user = new User("testuser", "test@test.com", "password", null);
    em.persistAndFlush(user);

    // when
    boolean result = userRepository.existsByEmail("test@test.com");

    // then
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("이메일 존재 여부 확인 - 미존재")
  void existsByEmail_false() {
    // when
    boolean result = userRepository.existsByEmail("nonexist@test.com");

    // then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("유저네임 존재 여부 확인 - 존재")
  void existsByUsername_true() {
    // given
    User user = new User("testuser", "test@test.com", "password", null);
    em.persistAndFlush(user);

    // when
    boolean result = userRepository.existsByUsername("testuser");

    // then
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("유저네임으로 조회 성공")
  void findByUsername_success() {
    // given
    User user = new User("testuser", "test@test.com", "password", null);
    em.persistAndFlush(user);

    // when
    var result = userRepository.findByUsername("testuser");

    // then
    assertThat(result).isPresent();
    assertThat(result.get().getEmail()).isEqualTo("test@test.com");
  }

  @Test
  @DisplayName("전체 유저 상세 조회 성공")
  void findAllWithDetails_success() {
    // given
    User user1 = new User("user1", "u1@test.com", "pw", null);
    User user2 = new User("user2", "u2@test.com", "pw", null);
    em.persistAndFlush(user1);
    em.persistAndFlush(user2);

    // when
    List<User> result = userRepository.findAllWithDetails();

    // then
    assertThat(result).hasSize(2);
  }

  @Test
  @DisplayName("전체 유저 상세 조회 - 비어있음")
  void findAllWithDetails_empty() {
    // when
    List<User> result = userRepository.findAllWithDetails();

    // then
    assertThat(result).isEmpty();
  }
}
