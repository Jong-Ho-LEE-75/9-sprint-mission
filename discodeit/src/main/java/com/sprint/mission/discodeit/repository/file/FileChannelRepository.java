package com.sprint.mission.discodeit.repository.file;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.repository.ChannelRepository;
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
 * 파일 기반 채널 저장소 구현체
 * ========================================
 *
 * Java 직렬화를 사용하여 채널 데이터를 파일 시스템에 저장합니다.
 * FileUserRepository와 동일한 패턴을 사용합니다.
 *
 * [저장 위치]
 * {discodeit.repository.file-directory}/Channel/{UUID}.ser
 * 예: ./data/Channel/550e8400-....ser
 *
 * [설정 방법]
 * application.properties:
 * discodeit.repository.type=file
 * discodeit.repository.file-directory=./data
 */
@Repository
@ConditionalOnProperty(name = "discodeit.repository.type", havingValue = "file")
public class FileChannelRepository implements ChannelRepository {
    /** 채널 파일을 저장하는 디렉토리 경로 */
    private final Path DIRECTORY;
    /** 저장 파일의 확장자 (.ser = Serialized) */
    private final String EXTENSION = ".ser";

    public FileChannelRepository(@Value("${discodeit.repository.file-directory}") String fileDirectory) {
        this.DIRECTORY = Paths.get(fileDirectory, Channel.class.getSimpleName());
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
     * 예: UUID "550e8400-..." → "./data/Channel/550e8400-....ser"
     */
    private Path resolvePath(UUID id) {
        return DIRECTORY.resolve(id + EXTENSION);
    }

    /**
     * 채널을 파일로 저장합니다. (직렬화)
     *
     * 새 채널이면 파일 생성, 기존 채널이면 파일 덮어쓰기 (Update).
     */
    @Override
    public Channel save(Channel channel) {
        Path path = resolvePath(channel.getId());
        try (
                FileOutputStream fos = new FileOutputStream(path.toFile());
                ObjectOutputStream oos = new ObjectOutputStream(fos)
        ) {
            oos.writeObject(channel);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return channel;
    }

    /**
     * ID로 채널을 조회합니다. (역직렬화)
     *
     * 파일이 없으면 빈 Optional 반환.
     */
    @Override
    public Optional<Channel> findById(UUID id) {
        Path path = resolvePath(id);
        if (Files.notExists(path)) {
            return Optional.empty();
        }
        try (
                FileInputStream fis = new FileInputStream(path.toFile());
                ObjectInputStream ois = new ObjectInputStream(fis)
        ) {
            return Optional.of((Channel) ois.readObject());
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 모든 채널을 조회합니다.
     *
     * 디렉토리 내 모든 .ser 파일을 읽어 Channel 객체로 변환합니다.
     */
    @Override
    public List<Channel> findAll() {
        try {
            return Files.list(DIRECTORY)
                    .filter(path -> path.toString().endsWith(EXTENSION))
                    .map(path -> {
                        try (
                                FileInputStream fis = new FileInputStream(path.toFile());
                                ObjectInputStream ois = new ObjectInputStream(fis)
                        ) {
                            return (Channel) ois.readObject();
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
     * ID로 채널 파일을 삭제합니다.
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
     * ID로 채널 존재 여부를 확인합니다.
     *
     * 파일이 존재하면 true 반환.
     */
    @Override
    public boolean existsById(UUID id) {
        Path path = resolvePath(id);
        return Files.exists(path);
    }
}
