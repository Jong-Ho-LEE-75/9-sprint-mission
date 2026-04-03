package com.sprint.mission.discodeit.service.basic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.sprint.mission.discodeit.dto.data.UserStatusDto;
import com.sprint.mission.discodeit.dto.request.UserStatusUpdateRequest;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.mapper.UserStatusMapper;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BasicUserStatusServiceTest {

  @Mock
  private UserStatusRepository userStatusRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private UserStatusMapper userStatusMapper;

  @InjectMocks
  private BasicUserStatusService userStatusService;

  private final UUID userId = UUID.randomUUID();
  private final Instant now = Instant.now();

  @Test
  @DisplayName("userId로 상태 업데이트 성공 - 기존 상태 존재")
  void updateByUserId_success_existing() {
    // given
    Instant newLastActiveAt = Instant.now();
    UserStatusUpdateRequest request = new UserStatusUpdateRequest(newLastActiveAt);
    User user = new User("user1", "user1@test.com", "password1", null);
    UserStatus userStatus = new UserStatus(user, now);
    UserStatusDto expectedDto = new UserStatusDto(UUID.randomUUID(), userId, newLastActiveAt);

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(userStatusRepository.findByUser_Id(userId)).willReturn(Optional.of(userStatus));
    given(userStatusMapper.toDto(userStatus)).willReturn(expectedDto);

    // when
    UserStatusDto result = userStatusService.updateByUserId(userId, request);

    // then
    assertThat(result.lastActiveAt()).isEqualTo(newLastActiveAt);
  }

  @Test
  @DisplayName("userId로 상태 업데이트 성공 - 상태 신규 생성")
  void updateByUserId_success_createNew() {
    // given
    Instant newLastActiveAt = Instant.now();
    UserStatusUpdateRequest request = new UserStatusUpdateRequest(newLastActiveAt);
    User user = new User("user1", "user1@test.com", "password1", null);
    UserStatus newStatus = new UserStatus(user, Instant.now());
    UserStatusDto expectedDto = new UserStatusDto(UUID.randomUUID(), userId, newLastActiveAt);

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(userStatusRepository.findByUser_Id(userId)).willReturn(Optional.empty());
    given(userStatusRepository.save(any(UserStatus.class))).willReturn(newStatus);
    given(userStatusMapper.toDto(any(UserStatus.class))).willReturn(expectedDto);

    // when
    UserStatusDto result = userStatusService.updateByUserId(userId, request);

    // then
    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("userId로 상태 업데이트 실패 - 사용자 없음")
  void updateByUserId_fail_userNotFound() {
    // given
    UserStatusUpdateRequest request = new UserStatusUpdateRequest(Instant.now());
    given(userRepository.findById(userId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> userStatusService.updateByUserId(userId, request))
        .isInstanceOf(NoSuchElementException.class);
  }
}
