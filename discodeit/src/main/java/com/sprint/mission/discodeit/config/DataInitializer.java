package com.sprint.mission.discodeit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.repository.UserStatusRepository;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.UUID;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Random;

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

        // 강제 초기화 모드 확인 (--force-init 인자)
        boolean forceInit = args.length > 0 && args[0].equals("--force-init");

        List<User> existingUsers = userRepository.findAll();

        if (forceInit && !existingUsers.isEmpty()) {
            log.info("강제 초기화 모드: 기존 사용자 {} 명 삭제 후 재생성", existingUsers.size());
            existingUsers.forEach(user -> {
                userRepository.deleteById(user.getId());
                userStatusRepository.deleteByUserId(user.getId());
                if (user.getProfileId() != null) {
                    binaryContentRepository.deleteById(user.getProfileId());
                }
            });
        } else if (!existingUsers.isEmpty()) {
            log.info("기존 사용자 {} 명이 존재하여 초기 데이터 로드를 건너뜁니다.", existingUsers.size());
            log.info("모든 데이터를 초기화하려면 애플리케이션을 --force-init 옵션과 함께 실행하세요.");
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
                byte[] imageData = loadProfileImageData(initialUser.profileImage(), initialUser.username());
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
     * 파일이 없으면 자동으로 랜덤 아바타 이미지를 생성합니다.
     */
    private byte[] loadProfileImageData(String filename, String username) {
        try {
            ClassPathResource imageResource = new ClassPathResource("data/profiles/" + filename);
            return Files.readAllBytes(imageResource.getFile().toPath());
        } catch (IOException e) {
            log.warn("프로필 이미지 파일 없음 ({}), {} 아바타 생성", filename, username);
            return generateRandomAvatar(username);
        }
    }

    /**
     * 랜덤 아바타 이미지를 생성합니다.
     */
    private byte[] generateRandomAvatar(String username) {
        try {
            Random random = new Random(username.hashCode());
            int size = 200;
            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();

            // 안티앨리어싱 적용
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 사용자별 고정된 배경색 (파스텔톤)
            Color[] pastelColors = {
                new Color(255, 179, 186), // 연한 분홍
                new Color(255, 223, 186), // 연한 주황
                new Color(255, 255, 186), // 연한 노랑
                new Color(186, 255, 201), // 연한 초록
                new Color(186, 225, 255), // 연한 파랑
                new Color(220, 198, 224), // 연한 보라
            };
            Color bgColor = pastelColors[Math.abs(username.hashCode()) % pastelColors.length];
            g2d.setColor(bgColor);
            g2d.fillRect(0, 0, size, size);

            // 이니셜 텍스트 (사용자 이름의 첫 글자)
            g2d.setColor(Color.WHITE);
            // 한글 폰트 지원
            g2d.setFont(new Font("맑은 고딕", Font.BOLD, 80));
            FontMetrics fm = g2d.getFontMetrics();
            String initial = username.substring(0, 1);
            int x = (size - fm.stringWidth(initial)) / 2;
            int y = ((size - fm.getHeight()) / 2) + fm.getAscent();
            g2d.drawString(initial, x, y);

            g2d.dispose();

            // PNG로 변환
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("랜덤 아바타 생성 실패: {}", username, e);
            return new byte[0];
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
