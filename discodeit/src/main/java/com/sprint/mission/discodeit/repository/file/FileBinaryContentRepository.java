package com.sprint.mission.discodeit.repository.file;

import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
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
 * 파일 기반 바이너리 콘텐츠 저장소 구현체
 * ========================================
 *
 * Java 직렬화를 사용하여 바이너리 콘텐츠(파일)를 파일 시스템에 저장합니다.
 *
 * [저장 위치]
 * {discodeit.repository.file-directory}/BinaryContent/{UUID}.ser
 *
 * [주의]
 * 파일 데이터(byte[])도 함께 직렬화되므로 큰 파일은 저장 공간을 많이 차지합니다.
 */
@Repository
@ConditionalOnProperty(name = "discodeit.repository.type", havingValue = "file")
public class FileBinaryContentRepository implements BinaryContentRepository {
    /** 바이너리 콘텐츠 파일을 저장하는 디렉토리 경로 */
    private final Path DIRECTORY;
    /** 저장 파일의 확장자 */
    private final String EXTENSION = ".ser";

    public FileBinaryContentRepository(@Value("${discodeit.repository.file-directory}") String fileDirectory) {
        this.DIRECTORY = Paths.get(fileDirectory, BinaryContent.class.getSimpleName());
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
     * 예: UUID "550e8400-..." → "./data/BinaryContent/550e8400-....ser"
     */
    private Path resolvePath(UUID id) {
        return DIRECTORY.resolve(id + EXTENSION);
    }

    /**
     * 바이너리 콘텐츠를 파일로 저장합니다. (직렬화)
     *
     * 새 콘텐츠면 파일 생성, 기존이면 파일 덮어쓰기 (Update).
     * byte[] 데이터도 함께 직렬화되어 저장됩니다.
     */
    @Override
    public BinaryContent save(BinaryContent binaryContent) {
        Path path = resolvePath(binaryContent.getId());
        try (
                FileOutputStream fos = new FileOutputStream(path.toFile());
                ObjectOutputStream oos = new ObjectOutputStream(fos)
        ) {
            oos.writeObject(binaryContent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return binaryContent;
    }

    /**
     * ID로 바이너리 콘텐츠를 조회합니다. (역직렬화)
     *
     * 파일이 없으면 빈 Optional 반환.
     */
    @Override
    public Optional<BinaryContent> findById(UUID id) {
        Path path = resolvePath(id);
        if (Files.notExists(path)) {
            return Optional.empty();
        }
        try (
                FileInputStream fis = new FileInputStream(path.toFile());
                ObjectInputStream ois = new ObjectInputStream(fis)
        ) {
            return Optional.of((BinaryContent) ois.readObject());
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 모든 바이너리 콘텐츠를 조회합니다.
     *
     * 디렉토리 내 모든 .ser 파일을 읽어 BinaryContent 객체로 변환합니다.
     * [주의] 파일이 많거나 큰 경우 메모리 사용량이 높아질 수 있습니다.
     */
    @Override
    public List<BinaryContent> findAll() {
        try {
            return Files.list(DIRECTORY)
                    .filter(path -> path.toString().endsWith(EXTENSION))
                    .map(path -> {
                        try (
                                FileInputStream fis = new FileInputStream(path.toFile());
                                ObjectInputStream ois = new ObjectInputStream(fis)
                        ) {
                            return (BinaryContent) ois.readObject();
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
     * 여러 ID로 바이너리 콘텐츠 목록을 조회합니다.
     *
     * 메시지의 첨부파일 목록을 한 번에 조회할 때 사용합니다.
     * 전체 콘텐츠를 조회한 후 ID로 필터링합니다.
     */
    @Override
    public List<BinaryContent> findAllByIdIn(List<UUID> ids) {
        return findAll().stream()
                .filter(bc -> ids.contains(bc.getId()))
                .toList();
    }

    /**
     * ID로 바이너리 콘텐츠 파일을 삭제합니다.
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
     * ID로 바이너리 콘텐츠 존재 여부를 확인합니다.
     *
     * 파일이 존재하면 true 반환.
     */
    @Override
    public boolean existsById(UUID id) {
        Path path = resolvePath(id);
        return Files.exists(path);
    }
}
