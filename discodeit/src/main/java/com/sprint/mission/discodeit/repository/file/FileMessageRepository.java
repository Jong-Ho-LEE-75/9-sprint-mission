package com.sprint.mission.discodeit.repository.file;

import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.repository.MessageRepository;
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
 * 파일 기반 메시지 저장소 구현체
 * ========================================
 *
 * Java 직렬화를 사용하여 메시지 데이터를 파일 시스템에 저장합니다.
 *
 * [저장 위치]
 * {discodeit.repository.file-directory}/Message/{UUID}.ser
 */
@Repository
@ConditionalOnProperty(name = "discodeit.repository.type", havingValue = "file")
public class FileMessageRepository implements MessageRepository {
    /** 메시지 파일을 저장하는 디렉토리 경로 */
    private final Path DIRECTORY;
    /** 저장 파일의 확장자 */
    private final String EXTENSION = ".ser";

    public FileMessageRepository(@Value("${discodeit.repository.file-directory}") String fileDirectory) {
        this.DIRECTORY = Paths.get(fileDirectory, Message.class.getSimpleName());
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
     * 예: UUID "550e8400-..." → "./data/Message/550e8400-....ser"
     */
    private Path resolvePath(UUID id) {
        return DIRECTORY.resolve(id + EXTENSION);
    }

    /**
     * 메시지를 파일로 저장합니다. (직렬화)
     *
     * 새 메시지면 파일 생성, 기존 메시지면 파일 덮어쓰기 (Update).
     */
    @Override
    public Message save(Message message) {
        Path path = resolvePath(message.getId());
        try (
                FileOutputStream fos = new FileOutputStream(path.toFile());
                ObjectOutputStream oos = new ObjectOutputStream(fos)
        ) {
            oos.writeObject(message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return message;
    }

    /**
     * ID로 메시지를 조회합니다. (역직렬화)
     *
     * 파일이 없으면 빈 Optional 반환.
     */
    @Override
    public Optional<Message> findById(UUID id) {
        Path path = resolvePath(id);
        if (Files.notExists(path)) {
            return Optional.empty();
        }
        try (
                FileInputStream fis = new FileInputStream(path.toFile());
                ObjectInputStream ois = new ObjectInputStream(fis)
        ) {
            return Optional.of((Message) ois.readObject());
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 모든 메시지를 조회합니다.
     *
     * 디렉토리 내 모든 .ser 파일을 읽어 Message 객체로 변환합니다.
     */
    @Override
    public List<Message> findAll() {
        try {
            return Files.list(DIRECTORY)
                    .filter(path -> path.toString().endsWith(EXTENSION))
                    .map(path -> {
                        try (
                                FileInputStream fis = new FileInputStream(path.toFile());
                                ObjectInputStream ois = new ObjectInputStream(fis)
                        ) {
                            return (Message) ois.readObject();
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
     * 특정 채널의 모든 메시지를 조회합니다.
     *
     * 전체 메시지를 조회한 후 channelId로 필터링합니다.
     * 채널 화면에서 메시지 목록을 표시할 때 사용합니다.
     */
    @Override
    public List<Message> findAllByChannelId(UUID channelId) {
        return findAll().stream()
                .filter(msg -> msg.getChannelId().equals(channelId))
                .toList();
    }

    /**
     * ID로 메시지 파일을 삭제합니다.
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
     * 특정 채널의 모든 메시지를 일괄 삭제합니다.
     *
     * 채널 삭제 시 해당 채널의 모든 메시지를 함께 삭제할 때 사용합니다.
     * 각 메시지의 파일을 개별적으로 삭제합니다.
     */
    @Override
    public void deleteAllByChannelId(UUID channelId) {
        findAllByChannelId(channelId).forEach(msg -> deleteById(msg.getId()));
    }

    /**
     * ID로 메시지 존재 여부를 확인합니다.
     *
     * 파일이 존재하면 true 반환.
     */
    @Override
    public boolean existsById(UUID id) {
        Path path = resolvePath(id);
        return Files.exists(path);
    }
}
