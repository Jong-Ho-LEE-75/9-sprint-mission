package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.request.ReadStatusCreateRequest;
import com.sprint.mission.discodeit.dto.request.ReadStatusUpdateRequest;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.ReadStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BasicReadStatusService implements ReadStatusService {
    private final ReadStatusRepository readStatusRepository;
    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;

    @Override
    public ReadStatus create(ReadStatusCreateRequest request) {
        // User 존재 여부 확인
        if (!userRepository.existsById(request.getUserId())) {
            throw new NoSuchElementException("User not found: " + request.getUserId());
        }
        // Channel 존재 여부 확인
        if (!channelRepository.existsById(request.getChannelId())) {
            throw new NoSuchElementException("Channel not found: " + request.getChannelId());
        }
        // 같은 User와 Channel의 ReadStatus가 이미 존재하는지 확인
        if (readStatusRepository.findByUserIdAndChannelId(request.getUserId(), request.getChannelId()).isPresent()) {
            throw new IllegalArgumentException("ReadStatus already exists for user and channel");
        }

        ReadStatus readStatus = new ReadStatus(
                request.getUserId(),
                request.getChannelId(),
                request.getLastReadAt()
        );
        return readStatusRepository.save(readStatus);
    }

    @Override
    public ReadStatus find(UUID id) {
        return readStatusRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("ReadStatus not found: " + id));
    }

    @Override
    public List<ReadStatus> findAllByUserId(UUID userId) {
        return readStatusRepository.findAllByUserId(userId);
    }

    @Override
    public ReadStatus update(UUID id, ReadStatusUpdateRequest request) {
        ReadStatus readStatus = readStatusRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("ReadStatus not found: " + id));
        readStatus.update(request.getLastReadAt());
        return readStatusRepository.save(readStatus);
    }

    @Override
    public void delete(UUID id) {
        readStatusRepository.deleteById(id);
    }
}