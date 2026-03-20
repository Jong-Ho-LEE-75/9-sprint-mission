package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.data.UserStatusDto;
import com.sprint.mission.discodeit.dto.request.UserStatusUpdateRequest;
import java.util.UUID;

public interface UserStatusService {

  UserStatusDto updateByUserId(UUID userId, UserStatusUpdateRequest request);
}
