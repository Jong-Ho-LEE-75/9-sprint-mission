package com.sprint.mission.discodeit.repository.file;

import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ========================================
 * 파일 기반 사용자 상태 저장소 구현체
 * ========================================
 *
 * Java 직렬화를 사용하여 사용자 상태 데이터를 파일 시스템에 저장합니다.
 *
 * [저장 위치]
 * {discodeit.repository.file-directory}/UserStatus/{UUID}.ser
 */
@Repository
@ConditionalOnProperty(name = "discodeit.repository.type", havingValue = "file")
public class FileUserStatusRepository implements UserStatusRepository {
    /** 사용자 상태 파일을 저장하는 디렉토리 경로 */
    private final Path DIRECTORY;
    /** 저장 파일의 확장자 */
    private final String EXTENSION = ".ser";

    public FileUserStatusRepository(@Value("${discodeit.repository.file-directory}") String fileDirectory) {
        this.DIRECTORY = Paths.get(fileDirectory, UserStatus.class.getSimpleName());
        if (Files.notExists(DIRECTORY)) {
            try {
                Files.createDirectories(DIRECTORY);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * ID를 파일 경로로 변환합니다.
     *
     * 예: UUID "550e8400-..." → "./data/UserStatus/550e8400-....ser"
     */
    private Path resolvePath(UUID id) {
        return DIRECTORY.resolve(id + EXTENSION);
    }

    /**
     * 사용자 상태를 파일로 저장합니다. (직렬화)
     *
     * 새 UserStatus면 파일 생성, 기존이면 파일 덮어쓰기 (Update).
     */
    @Override
    public UserStatus save(UserStatus userStatus) {
        Path path = resolvePath(userStatus.getId());
        try (
                FileOutputStream fos = new FileOutputStream(path.toFile());
                ObjectOutputStream oos = new ObjectOutputStream(fos)
        ) {
            oos.writeObject(userStatus);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return userStatus;
    }

    /**
     * ID로 사용자 상태를 조회합니다. (역직렬화)
     *
     * 파일이 없으면 빈 Optional 반환.
     */
    @Override
    public Optional<UserStatus> findById(UUID id) {
        Path path = resolvePath(id);
        if (Files.notExists(path)) {
            return Optional.empty();
        }
        try (
                FileInputStream fis = new FileInputStream(path.toFile());
                ObjectInputStream ois = new ObjectInputStream(fis)
        ) {
            return Optional.of((UserStatus) ois.readObject());
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 사용자 ID로 상태를 조회합니다.
     *
     * User의 온라인 상태를 확인할 때 주로 사용합니다.
     */
    @Override
    public Optional<UserStatus> findByUserId(UUID userId) {
        return findAll().stream()
                .filter(us -> us.getUserId().equals(userId))
                .findFirst();
    }

    /**
     * 모든 사용자 상태를 조회합니다.
     *
     * 디렉토리 내 모든 .ser 파일을 읽어 UserStatus 객체로 변환합니다.
     */
    @Override
    public List<UserStatus> findAll() {
        try {
            return Files.list(DIRECTORY)
                    .filter(path -> path.toString().endsWith(EXTENSION))
                    .map(path -> {
                        try (
                                FileInputStream fis = new FileInputStream(path.toFile());
                                ObjectInputStream ois = new ObjectInputStream(fis)
                        ) {
                            return (UserStatus) ois.readObject();
                        } catch (IOException | ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * ID로 사용자 상태 파일을 삭제합니다.
     */
    @Override
    public void deleteById(UUID id) {
        Path path = resolvePath(id);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 사용자 ID로 상태를 삭제합니다.
     *
     * User 삭제 시 연관된 UserStatus도 함께 삭제할 때 사용합니다.
     * ifPresent: Optional에 값이 있으면 람다 실행
     */
    @Override
    public void deleteByUserId(UUID userId) {
        findByUserId(userId).ifPresent(us -> deleteById(us.getId()));
    }

    /**
     * ID로 사용자 상태 존재 여부를 확인합니다.
     *
     * 파일이 존재하면 true 반환.
     */
    @Override
    public boolean existsById(UUID id) {
        Path path = resolvePath(id);
        return Files.exists(path);
    }

    /**
     * 사용자 ID로 상태 존재 여부를 확인합니다.
     *
     * UserStatus 중복 생성 방지에 사용합니다.
     */
    @Override
    public boolean existsByUserId(UUID userId) {
        return findAll().stream()
                .anyMatch(us -> us.getUserId().equals(userId));
    }
}
