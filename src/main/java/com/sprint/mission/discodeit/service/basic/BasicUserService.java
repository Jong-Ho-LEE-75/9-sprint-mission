package com.sprint.mission.discodeit.service.basic;

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
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class BasicUserService implements UserService {

  private final UserRepository userRepository;
  private final BinaryContentRepository binaryContentRepository;
  private final UserStatusRepository userStatusRepository;
  private final BinaryContentStorage binaryContentStorage;
  private final UserMapper userMapper;
  private final EntityManager entityManager;

  @Override
  @Transactional
  public UserDto create(UserCreateRequest userCreateRequest,
      Optional<BinaryContentCreateRequest> optionalProfileCreateRequest) {
    String username = userCreateRequest.username();
    String email = userCreateRequest.email();

    if (userRepository.existsByEmail(email)) {
      throw new IllegalArgumentException("User with email " + email + " already exists");
    }
    if (userRepository.existsByUsername(username)) {
      throw new IllegalArgumentException("User with username " + username + " already exists");
    }

    BinaryContent profile = optionalProfileCreateRequest
        .map(req -> {
          BinaryContent bc = new BinaryContent(req.fileName(), (long) req.bytes().length,
              req.contentType());
          bc = binaryContentRepository.save(bc);
          binaryContentStorage.put(bc.getId(), req.bytes());
          return bc;
        })
        .orElse(null);

    User user = new User(username, email, userCreateRequest.password(), profile);
    user = userRepository.save(user);
    userStatusRepository.save(new UserStatus(user, Instant.now()));

    // JPA 1차 캐시 무효화 후 UserStatus가 로드된 user를 재조회
    entityManager.flush();
    entityManager.refresh(user);
    return userMapper.toDto(user);
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserDto> findAll() {
    return userRepository.findAllWithDetails().stream()
        .map(userMapper::toDto)
        .toList();
  }

  @Override
  @Transactional
  public UserDto update(UUID userId, UserUpdateRequest userUpdateRequest,
      Optional<BinaryContentCreateRequest> optionalProfileCreateRequest) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new NoSuchElementException("User with id " + userId + " not found"));

    String newUsername = userUpdateRequest.newUsername();
    String newEmail = userUpdateRequest.newEmail();

    if (newEmail != null && !newEmail.equals(user.getEmail())
        && userRepository.existsByEmail(newEmail)) {
      throw new IllegalArgumentException("User with email " + newEmail + " already exists");
    }
    if (newUsername != null && !newUsername.equals(user.getUsername())
        && userRepository.existsByUsername(newUsername)) {
      throw new IllegalArgumentException("User with username " + newUsername + " already exists");
    }

    BinaryContent newProfile = optionalProfileCreateRequest
        .map(req -> {
          if (user.getProfile() != null) {
            UUID oldProfileId = user.getProfile().getId();
            user.clearProfile();
            binaryContentRepository.flush();
            binaryContentRepository.deleteById(oldProfileId);
            binaryContentStorage.delete(oldProfileId);
          }
          BinaryContent bc = new BinaryContent(req.fileName(), (long) req.bytes().length,
              req.contentType());
          bc = binaryContentRepository.save(bc);
          binaryContentStorage.put(bc.getId(), req.bytes());
          return bc;
        })
        .orElse(null);

    user.update(newUsername, newEmail, userUpdateRequest.newPassword(), newProfile);

    return userMapper.toDto(user);
  }

  @Override
  @Transactional
  public void delete(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new NoSuchElementException("User with id " + userId + " not found"));

    if (user.getProfile() != null) {
      UUID profileId = user.getProfile().getId();
      user.clearProfile();
      binaryContentRepository.flush();
      binaryContentRepository.deleteById(profileId);
      binaryContentStorage.delete(profileId);
    }

    userRepository.delete(user);
  }
}
