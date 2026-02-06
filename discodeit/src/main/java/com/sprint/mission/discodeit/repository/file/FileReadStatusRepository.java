package com.sprint.mission.discodeit.repository.file;

import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
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
 * 파일 기반 읽기 상태 저장소 구현체
 * ========================================
 *
 * Java 직렬화를 사용하여 읽기 상태 데이터를 파일 시스템에 저장합니다.
 *
 * [저장 위치]
 * {discodeit.repository.file-directory}/ReadStatus/{UUID}.ser
 */
@Repository
@ConditionalOnProperty(name = "discodeit.repository.type", havingValue = "file")
public class FileReadStatusRepository implements ReadStatusRepository {
    /** 읽기 상태 파일을 저장하는 디렉토리 경로 */
    private final Path DIRECTORY;
    /** 저장 파일의 확장자 */
    private final String EXTENSION = ".ser";

    public FileReadStatusRepository(@Value("${discodeit.repository.file-directory}") String fileDirectory) {
        this.DIRECTORY = Paths.get(fileDirectory, ReadStatus.class.getSimpleName());
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
     * 예: UUID "550e8400-..." → "./data/ReadStatus/550e8400-....ser"
     */
    private Path resolvePath(UUID id) {
        return DIRECTORY.resolve(id + EXTENSION);
    }

    /**
     * 읽기 상태를 파일로 저장합니다. (직렬화)
     *
     * 새 ReadStatus면 파일 생성, 기존이면 파일 덮어쓰기 (Update).
     */
    @Override
    public ReadStatus save(ReadStatus readStatus) {
        Path path = resolvePath(readStatus.getId());
        try (
                FileOutputStream fos = new FileOutputStream(path.toFile());
                ObjectOutputStream oos = new ObjectOutputStream(fos)
        ) {
            oos.writeObject(readStatus);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return readStatus;
    }

    /**
     * ID로 읽기 상태를 조회합니다. (역직렬화)
     *
     * 파일이 없으면 빈 Optional 반환.
     */
    @Override
    public Optional<ReadStatus> findById(UUID id) {
        Path path = resolvePath(id);
        if (Files.notExists(path)) {
            return Optional.empty();
        }
        try (
                FileInputStream fis = new FileInputStream(path.toFile());
                ObjectInputStream ois = new ObjectInputStream(fis)
        ) {
            return Optional.of((ReadStatus) ois.readObject());
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 모든 읽기 상태를 조회합니다.
     *
     * 디렉토리 내 모든 .ser 파일을 읽어 ReadStatus 객체로 변환합니다.
     */
    @Override
    public List<ReadStatus> findAll() {
        try {
            return Files.list(DIRECTORY)
                    .filter(path -> path.toString().endsWith(EXTENSION))
                    .map(path -> {
                        try (
                                FileInputStream fis = new FileInputStream(path.toFile());
                                ObjectInputStream ois = new ObjectInputStream(fis)
                        ) {
                            return (ReadStatus) ois.readObject();
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
     * 특정 사용자의 모든 읽기 상태를 조회합니다.
     *
     * 사용자가 참여 중인 채널 목록을 알아낼 때 사용합니다.
     */
    @Override
    public List<ReadStatus> findAllByUserId(UUID userId) {
        return findAll().stream()
                .filter(rs -> rs.getUserId().equals(userId))
                .toList();
    }

    /**
     * 특정 채널의 모든 읽기 상태를 조회합니다.
     *
     * PRIVATE 채널의 참여자 목록을 알아낼 때 사용합니다.
     */
    @Override
    public List<ReadStatus> findAllByChannelId(UUID channelId) {
        return findAll().stream()
                .filter(rs -> rs.getChannelId().equals(channelId))
                .toList();
    }

    /**
     * 사용자 ID와 채널 ID로 읽기 상태를 조회합니다.
     *
     * 두 조건을 AND로 결합하여 필터링합니다.
     * userId + channelId 조합은 유일하므로 결과는 0개 또는 1개입니다.
     */
    @Override
    public Optional<ReadStatus> findByUserIdAndChannelId(UUID userId, UUID channelId) {
        return findAll().stream()
                .filter(rs -> rs.getUserId().equals(userId) && rs.getChannelId().equals(channelId))
                .findFirst();
    }

    /**
     * ID로 읽기 상태 파일을 삭제합니다.
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
     * 특정 채널의 모든 읽기 상태를 일괄 삭제합니다.
     *
     * 채널 삭제 시 관련 ReadStatus를 모두 삭제할 때 사용합니다.
     */
    @Override
    public void deleteAllByChannelId(UUID channelId) {
        findAllByChannelId(channelId).forEach(rs -> deleteById(rs.getId()));
    }

    /**
     * ID로 읽기 상태 존재 여부를 확인합니다.
     *
     * 파일이 존재하면 true 반환.
     */
    @Override
    public boolean existsById(UUID id) {
        Path path = resolvePath(id);
        return Files.exists(path);
    }
}
