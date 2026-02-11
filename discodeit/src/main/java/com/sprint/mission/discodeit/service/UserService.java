package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserUpdateRequest;
import com.sprint.mission.discodeit.dto.response.UserResponse;
import java.util.List;
import java.util.UUID;

public interface UserService {
    UserResponse create(UserCreateRequest request, BinaryContentCreateRequest profileRequest);
    UserResponse find(UUID id);
    List<UserResponse> findAll();
    UserResponse update(UUID id, UserUpdateRequest request, BinaryContentCreateRequest profileRequest);
    void delete(UUID id);
}