package com.sprint.mission.discodeit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.repository.UserStatusRepository;

import java.time.Instant;
import java.util.UUID;
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

    private final UserRepository userRepository;
    private final BinaryContentRepository binaryContentRepository;
    private final UserStatusRepository userStatusRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) throws Exception {
        log.info("=== 초기 데이터 로드 시작 ===");

        // 이미 사용자가 있으면 초기화하지 않음
        if (!userRepository.findAll().isEmpty()) {
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
     * 사용자와 프로필 이미지를 생성합니다 (고정 UUID 사용).
     */
    private void createUser(InitialUser initialUser) {
        try {
            // 1. 프로필 이미지 생성 (고정 UUID 사용)
            UUID profileId = null;
            if (initialUser.profileImage() != null && initialUser.profileId() != null) {
                byte[] imageData = loadProfileImageData(initialUser.profileImage());
                if (imageData != null) {
                    String contentType = getContentType(initialUser.profileImage());
                    BinaryContent profile = new BinaryContent(
                            initialUser.profileId(),
                            initialUser.profileImage(),
                            contentType,
                            imageData
                    );
                    binaryContentRepository.save(profile);
                    profileId = initialUser.profileId();
                }
            }

            // 2. 사용자 생성 (고정 UUID 사용)
            User user = new User(
                    initialUser.id(),
                    initialUser.username(),
                    initialUser.email(),
                    initialUser.password(),
                    profileId
            );
            userRepository.save(user);

            // 3. 사용자 상태 생성 (온라인 상태로 설정)
            UserStatus userStatus = new UserStatus(initialUser.id(), Instant.now());
            userStatusRepository.save(userStatus);

            log.info("사용자 생성 완료: {} ({})", initialUser.username(), initialUser.email());

        } catch (Exception e) {
            log.error("사용자 생성 실패: {} - {}", initialUser.username(), e.getMessage());
        }
    }

    /**
     * 프로필 이미지 파일 데이터를 로드합니다.
     */
    private byte[] loadProfileImageData(String filename) {
        try {
            ClassPathResource imageResource = new ClassPathResource("data/profiles/" + filename);
            return Files.readAllBytes(imageResource.getFile().toPath());
        } catch (IOException e) {
            log.error("프로필 이미지 로드 실패: {}", filename, e);
            return null;
        }
    }

    /**
     * 파일 확장자로 Content-Type을 결정합니다.
     */
    private String getContentType(String filename) {
        if (filename.endsWith(".png")) {
            return "image/png";
        } else if (filename.endsWith(".gif")) {
            return "image/gif";
        }
        return "image/jpeg";
    }

    /**
     * 초기 사용자 데이터 DTO
     */
    private record InitialUser(
            UUID id,
            String username,
            String email,
            String password,
            String profileImage,
            UUID profileId
    ) {
    }
}
