package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.data.UserStatusDto;
import com.sprint.mission.discodeit.dto.request.UserStatusUpdateRequest;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.mapper.UserStatusMapper;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
import com.sprint.mission.discodeit.service.UserStatusService;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class BasicUserStatusService implements UserStatusService {

  private final UserStatusRepository userStatusRepository;
  private final UserRepository userRepository;
  private final UserStatusMapper userStatusMapper;

  @Override
  @Transactional(readOnly = true)
  public UserStatusDto findByUserId(UUID userId) {
    UserStatus userStatus = userStatusRepository.findByUser_Id(userId)
        .orElseThrow(
            () -> new NoSuchElementException("UserStatus with userId " + userId + " not found"));
    return userStatusMapper.toDto(userStatus);
  }

  @Override
  @Transactional
  public UserStatusDto updateByUserId(UUID userId, UserStatusUpdateRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(
            () -> new NoSuchElementException("UserStatus with userId " + userId + " not found"));

    // UserStatus가 없으면 새로 생성 (데이터 불일치 복구)
    UserStatus userStatus = userStatusRepository.findByUser_Id(userId)
        .orElseGet(() -> userStatusRepository.save(new UserStatus(user, Instant.now())));

    userStatus.update(request.newLastActiveAt());
    return userStatusMapper.toDto(userStatus);
  }
}
