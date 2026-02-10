package com.sprint.mission.discodeit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserStatusUpdateRequest;
import com.sprint.mission.discodeit.dto.response.UserResponse;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.service.UserStatusService;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * 애플리케이션 시작 시 초기 데이터를 로드하는 클래스
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;
    private final UserStatusService userStatusService;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) throws Exception {
        log.info("=== 초기 데이터 로드 시작 ===");

        // 이미 사용자가 있으면 초기화하지 않음
        if (!userService.findAll().isEmpty()) {
            log.info("기존 사용자가 존재하여 초기 데이터 로드를 건너뜁니다.");
            return;
        }

        loadInitialUsers();
        log.info("=== 초기 데이터 로드 완료 ===");
    }

    /**
     * users.json 파일에서 초기 사용자 데이터를 로드합니다.
     */
    private void loadInitialUsers() {
        try {
            // users.json 파일 읽기
            ClassPathResource resource = new ClassPathResource("data/users.json");
            List<InitialUser> initialUsers = objectMapper.readValue(
                    resource.getInputStream(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, InitialUser.class)
            );

            // 각 사용자 생성
            for (InitialUser initialUser : initialUsers) {
                createUser(initialUser);
            }

            log.info("총 {}명의 사용자가 생성되었습니다.", initialUsers.size());

        } catch (IOException e) {
            log.error("초기 사용자 데이터 로드 실패", e);
        }
    }

    /**
     * 사용자와 프로필 이미지를 생성합니다.
     */
    private void createUser(InitialUser initialUser) {
        try {
            // 사용자 생성 요청 객체
            UserCreateRequest userRequest = new UserCreateRequest(
                    initialUser.username(),
                    initialUser.email(),
                    initialUser.password()
            );

            // 프로필 이미지 로드
            BinaryContentCreateRequest profileRequest = null;
            if (initialUser.profileImage() != null) {
                profileRequest = loadProfileImage(initialUser.profileImage());
            }

            // 사용자 생성
            UserResponse user = userService.create(userRequest, profileRequest);

            // 온라인 상태 업데이트 (현재 시간으로 설정)
            updateUserOnlineStatus(user.id());

            log.info("사용자 생성 완료: {} ({})", initialUser.username(), initialUser.email());

        } catch (Exception e) {
            log.error("사용자 생성 실패: {} - {}", initialUser.username(), e.getMessage());
        }
    }

    /**
     * 프로필 이미지 파일을 로드합니다.
     */
    private BinaryContentCreateRequest loadProfileImage(String filename) {
        try {
            ClassPathResource imageResource = new ClassPathResource("data/profiles/" + filename);
            byte[] imageData = Files.readAllBytes(imageResource.getFile().toPath());

            // 파일 확장자로 Content-Type 결정
            String contentType = "image/jpeg";
            if (filename.endsWith(".png")) {
                contentType = "image/png";
            } else if (filename.endsWith(".gif")) {
                contentType = "image/gif";
            }

            return new BinaryContentCreateRequest(filename, contentType, imageData);

        } catch (IOException e) {
            log.error("프로필 이미지 로드 실패: {}", filename, e);
            return null;
        }
    }

    /**
     * 사용자를 온라인 상태로 업데이트합니다.
     */
    private void updateUserOnlineStatus(java.util.UUID userId) {
        try {
            UserStatusUpdateRequest statusRequest = new UserStatusUpdateRequest(Instant.now());
            userStatusService.updateByUserId(userId, statusRequest);
            log.debug("사용자 온라인 상태 업데이트 완료: {}", userId);
        } catch (Exception e) {
            log.error("온라인 상태 업데이트 실패: {}", userId, e);
        }
    }

    /**
     * 초기 사용자 데이터 DTO
     */
    private record InitialUser(
            String username,
            String email,
            String password,
            String profileImage
    ) {
    }
}
