package com.sprint.mission.discodeit.service.basic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.never;

import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserUpdateRequest;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import jakarta.persistence.EntityManager;
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
class BasicUserServiceTest {

  @InjectMocks
  private BasicUserService userService;

  @Mock
  private UserRepository userRepository;
  @Mock
  private BinaryContentRepository binaryContentRepository;
  @Mock
  private UserStatusRepository userStatusRepository;
  @Mock
  private BinaryContentStorage binaryContentStorage;
  @Mock
  private UserMapper userMapper;
  @Mock
  private EntityManager entityManager;

  @Test
  @DisplayName("유저 생성 성공")
  void create_success() {
    // given
    UserCreateRequest request = new UserCreateRequest("testuser", "test@test.com", "password");
    User user = new User("testuser", "test@test.com", "password", null);
    UUID userId = UUID.randomUUID();
    ReflectionTestUtils.setField(user, "id", userId);

    UserDto expectedDto = new UserDto(userId, "testuser", "test@test.com", null, true);

    given(userRepository.existsByEmail("test@test.com")).willReturn(false);
    given(userRepository.existsByUsername("testuser")).willReturn(false);
    given(userRepository.save(any(User.class))).willReturn(user);
    given(userStatusRepository.save(any(UserStatus.class))).willReturn(null);
    willDoNothing().given(entityManager).flush();
    willDoNothing().given(entityManager).refresh(any());
    given(userMapper.toDto(any(User.class))).willReturn(expectedDto);

    // when
    UserDto result = userService.create(request, Optional.empty());

    // then
    assertThat(result.username()).isEqualTo("testuser");
    assertThat(result.email()).isEqualTo("test@test.com");
    then(userRepository).should().save(any(User.class));
    then(userStatusRepository).should().save(any(UserStatus.class));
  }

  @Test
  @DisplayName("유저 생성 실패 - 이메일 중복")
  void create_fail_duplicateEmail() {
    // given
    UserCreateRequest request = new UserCreateRequest("testuser", "dup@test.com", "password");
    given(userRepository.existsByEmail("dup@test.com")).willReturn(true);

    // when & then
    assertThatThrownBy(() -> userService.create(request, Optional.empty()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("dup@test.com");
  }

  @Test
  @DisplayName("유저 생성 실패 - 유저네임 중복")
  void create_fail_duplicateUsername() {
    // given
    UserCreateRequest request = new UserCreateRequest("dupuser", "test@test.com", "password");
    given(userRepository.existsByEmail("test@test.com")).willReturn(false);
    given(userRepository.existsByUsername("dupuser")).willReturn(true);

    // when & then
    assertThatThrownBy(() -> userService.create(request, Optional.empty()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("dupuser");
  }

  @Test
  @DisplayName("유저 생성 성공 - 프로필 이미지 포함")
  void create_success_withProfile() {
    // given
    UserCreateRequest request = new UserCreateRequest("testuser", "test@test.com", "password");
    BinaryContentCreateRequest profileReq = new BinaryContentCreateRequest("profile.png",
        "image/png", new byte[]{1, 2, 3});
    BinaryContent profile = new BinaryContent("profile.png", 3L, "image/png");
    UUID profileId = UUID.randomUUID();
    ReflectionTestUtils.setField(profile, "id", profileId);

    User user = new User("testuser", "test@test.com", "password", profile);
    UUID userId = UUID.randomUUID();
    ReflectionTestUtils.setField(user, "id", userId);

    UserDto expectedDto = new UserDto(userId, "testuser", "test@test.com", null, true);

    given(userRepository.existsByEmail("test@test.com")).willReturn(false);
    given(userRepository.existsByUsername("testuser")).willReturn(false);
    given(binaryContentRepository.save(any(BinaryContent.class))).willReturn(profile);
    given(binaryContentStorage.put(any(UUID.class), any(byte[].class))).willReturn(profileId);
    given(userRepository.save(any(User.class))).willReturn(user);
    given(userStatusRepository.save(any(UserStatus.class))).willReturn(null);
    willDoNothing().given(entityManager).flush();
    willDoNothing().given(entityManager).refresh(any());
    given(userMapper.toDto(any(User.class))).willReturn(expectedDto);

    // when
    UserDto result = userService.create(request, Optional.of(profileReq));

    // then
    assertThat(result).isNotNull();
    then(binaryContentRepository).should().save(any(BinaryContent.class));
    then(binaryContentStorage).should().put(any(UUID.class), any(byte[].class));
  }

  @Test
  @DisplayName("유저 수정 성공")
  void update_success() {
    // given
    UUID userId = UUID.randomUUID();
    User user = new User("oldname", "old@test.com", "password", null);
    ReflectionTestUtils.setField(user, "id", userId);

    UserUpdateRequest request = new UserUpdateRequest("newname", "new@test.com", null);
    UserDto expectedDto = new UserDto(userId, "newname", "new@test.com", null, true);

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(userRepository.existsByEmail("new@test.com")).willReturn(false);
    given(userRepository.existsByUsername("newname")).willReturn(false);
    given(userMapper.toDto(any(User.class))).willReturn(expectedDto);

    // when
    UserDto result = userService.update(userId, request, Optional.empty());

    // then
    assertThat(result.username()).isEqualTo("newname");
    assertThat(result.email()).isEqualTo("new@test.com");
  }

  @Test
  @DisplayName("유저 수정 실패 - 이메일 중복")
  void update_fail_duplicateEmail() {
    // given
    UUID userId = UUID.randomUUID();
    User user = new User("testuser", "old@test.com", "password", null);
    ReflectionTestUtils.setField(user, "id", userId);

    UserUpdateRequest request = new UserUpdateRequest(null, "dup@test.com", null);

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(userRepository.existsByEmail("dup@test.com")).willReturn(true);

    // when & then
    assertThatThrownBy(() -> userService.update(userId, request, Optional.empty()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("dup@test.com");
  }

  @Test
  @DisplayName("유저 수정 실패 - 유저 없음")
  void update_fail_notFound() {
    // given
    UUID userId = UUID.randomUUID();
    UserUpdateRequest request = new UserUpdateRequest("newname", null, null);
    given(userRepository.findById(userId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> userService.update(userId, request, Optional.empty()))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  @DisplayName("유저 수정 실패 - 유저네임 중복")
  void update_fail_duplicateUsername() {
    // given
    UUID userId = UUID.randomUUID();
    User user = new User("testuser", "test@test.com", "password", null);
    ReflectionTestUtils.setField(user, "id", userId);

    UserUpdateRequest request = new UserUpdateRequest("dupname", null, null);

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(userRepository.existsByUsername("dupname")).willReturn(true);

    // when & then
    assertThatThrownBy(() -> userService.update(userId, request, Optional.empty()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("dupname");
  }

  @Test
  @DisplayName("유저 수정 성공 - 프로필 이미지 교체")
  void update_success_withProfileReplacement() {
    // given
    UUID userId = UUID.randomUUID();
    BinaryContent oldProfile = new BinaryContent("old.png", 100L, "image/png");
    UUID oldProfileId = UUID.randomUUID();
    ReflectionTestUtils.setField(oldProfile, "id", oldProfileId);

    User user = new User("testuser", "test@test.com", "password", oldProfile);
    ReflectionTestUtils.setField(user, "id", userId);

    BinaryContentCreateRequest newProfileReq = new BinaryContentCreateRequest("new.png",
        "image/png", new byte[]{1, 2, 3});
    BinaryContent newProfile = new BinaryContent("new.png", 3L, "image/png");
    UUID newProfileId = UUID.randomUUID();
    ReflectionTestUtils.setField(newProfile, "id", newProfileId);

    UserUpdateRequest request = new UserUpdateRequest(null, null, null);
    UserDto expectedDto = new UserDto(userId, "testuser", "test@test.com", null, true);

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(binaryContentRepository.save(any(BinaryContent.class))).willReturn(newProfile);
    given(binaryContentStorage.put(any(UUID.class), any(byte[].class))).willReturn(newProfileId);
    given(userMapper.toDto(any(User.class))).willReturn(expectedDto);

    // when
    UserDto result = userService.update(userId, request, Optional.of(newProfileReq));

    // then
    assertThat(result).isNotNull();
    then(binaryContentRepository).should().flush();
    then(binaryContentRepository).should().deleteById(oldProfileId);
    then(binaryContentStorage).should().delete(oldProfileId);
    then(binaryContentRepository).should().save(any(BinaryContent.class));
  }

  @Test
  @DisplayName("유저 삭제 성공")
  void delete_success() {
    // given
    UUID userId = UUID.randomUUID();
    User user = new User("testuser", "test@test.com", "password", null);
    ReflectionTestUtils.setField(user, "id", userId);
    given(userRepository.findById(userId)).willReturn(Optional.of(user));

    // when
    userService.delete(userId);

    // then
    then(userRepository).should().delete(user);
  }

  @Test
  @DisplayName("유저 삭제 성공 - 프로필 이미지 포함")
  void delete_success_withProfile() {
    // given
    UUID userId = UUID.randomUUID();
    BinaryContent profile = new BinaryContent("profile.png", 100L, "image/png");
    UUID profileId = UUID.randomUUID();
    ReflectionTestUtils.setField(profile, "id", profileId);

    User user = new User("testuser", "test@test.com", "password", profile);
    ReflectionTestUtils.setField(user, "id", userId);

    given(userRepository.findById(userId)).willReturn(Optional.of(user));

    // when
    userService.delete(userId);

    // then
    then(binaryContentRepository).should().flush();
    then(binaryContentRepository).should().deleteById(profileId);
    then(binaryContentStorage).should().delete(profileId);
    then(userRepository).should().delete(user);
  }

  @Test
  @DisplayName("유저 삭제 실패 - 유저 없음")
  void delete_fail_notFound() {
    // given
    UUID userId = UUID.randomUUID();
    given(userRepository.findById(userId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> userService.delete(userId))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  @DisplayName("전체 유저 조회 성공")
  void findAll_success() {
    // given
    User user1 = new User("user1", "u1@test.com", "pw", null);
    User user2 = new User("user2", "u2@test.com", "pw", null);
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    ReflectionTestUtils.setField(user1, "id", id1);
    ReflectionTestUtils.setField(user2, "id", id2);

    UserDto dto1 = new UserDto(id1, "user1", "u1@test.com", null, true);
    UserDto dto2 = new UserDto(id2, "user2", "u2@test.com", null, true);

    given(userRepository.findAllWithDetails()).willReturn(List.of(user1, user2));
    given(userMapper.toDto(user1)).willReturn(dto1);
    given(userMapper.toDto(user2)).willReturn(dto2);

    // when
    List<UserDto> result = userService.findAll();

    // then
    assertThat(result).hasSize(2);
    assertThat(result.get(0).username()).isEqualTo("user1");
  }
}
