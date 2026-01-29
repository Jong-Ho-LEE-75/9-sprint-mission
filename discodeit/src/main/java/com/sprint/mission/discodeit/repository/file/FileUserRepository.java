package com.sprint.mission.discodeit.repository.file;

import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.repository.UserRepository;
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
 * 파일 기반 사용자 저장소 구현체
 * ========================================
 *
 * 이 클래스는 UserRepository 인터페이스의 구현체로,
 * Java 직렬화(Serialization)를 사용하여 파일 시스템에 데이터를 저장합니다.
 *
 * [파일 저장 방식]
 * 각 User 객체를 개별 파일로 저장합니다.
 * 파일명: {UUID}.ser
 * 예: 550e8400-e29b-41d4-a716-446655440000.ser
 *
 * [직렬화(Serialization)란?]
 * Java 객체를 바이트 스트림으로 변환하는 과정입니다.
 * 이 바이트 스트림을 파일로 저장하면 프로그램 종료 후에도 데이터가 유지됩니다.
 *
 * 객체 → 직렬화(Serialize) → 바이트 스트림 → 파일 저장
 * 파일 → 읽기 → 바이트 스트림 → 역직렬화(Deserialize) → 객체
 *
 * [Serializable 인터페이스]
 * 직렬화하려면 클래스가 Serializable 인터페이스를 구현해야 합니다.
 * User 클래스는 BaseEntity를 통해 Serializable을 구현합니다.
 *
 * [JCF vs File 저장소 비교]
 * JCF (메모리):
 * - 장점: 빠름, 설정 간단
 * - 단점: 프로그램 종료 시 데이터 삭제
 *
 * File (파일):
 * - 장점: 프로그램 종료 후에도 데이터 유지
 * - 단점: 느림, 파일 I/O 비용
 *
 * [어노테이션 설명]
 * @Repository: Spring Bean으로 등록
 * @ConditionalOnProperty: discodeit.repository.type=file 일 때만 활성화
 *
 * [설정 방법]
 * application.properties:
 * discodeit.repository.type=file
 * discodeit.repository.file-directory=./data
 */
@Repository
@ConditionalOnProperty(name = "discodeit.repository.type", havingValue = "file")
public class FileUserRepository implements UserRepository {
    /**
     * 사용자 파일을 저장할 디렉토리 경로
     *
     * 설정 파일의 discodeit.repository.file-directory 값 + "/User"
     * 예: ./data/User/
     */
    private final Path DIRECTORY;

    /**
     * 저장 파일의 확장자
     *
     * .ser은 직렬화된(Serialized) 파일을 나타내는 관례적인 확장자입니다.
     */
    private final String EXTENSION = ".ser";

    /**
     * ========================================
     * 생성자
     * ========================================
     *
     * @Value 어노테이션으로 application.properties의 값을 주입받습니다.
     *
     * @param fileDirectory 파일 저장 기본 디렉토리 (설정 파일에서 주입)
     *
     * [생성자에서 하는 일]
     * 1. 저장 디렉토리 경로 설정 (기본 경로 + "/User")
     * 2. 디렉토리가 없으면 생성
     */
    public FileUserRepository(@Value("${discodeit.repository.file-directory}") String fileDirectory) {
        // Paths.get(): 문자열을 Path 객체로 변환
        // 예: "./data" + "User" → "./data/User"
        this.DIRECTORY = Paths.get(fileDirectory, User.class.getSimpleName());

        // 디렉토리가 존재하지 않으면 생성
        if (Files.notExists(DIRECTORY)) {
            try {
                // createDirectories: 필요한 모든 상위 디렉토리도 함께 생성
                Files.createDirectories(DIRECTORY);
            } catch (IOException e) {
                // 디렉토리 생성 실패 시 RuntimeException으로 감싸서 던짐
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * ID를 파일 경로로 변환합니다.
     *
     * 예: UUID "550e8400-..." → "./data/User/550e8400-....ser"
     */
    private Path resolvePath(UUID id) {
        return DIRECTORY.resolve(id + EXTENSION);
    }

    /**
     * ========================================
     * 사용자를 파일로 저장합니다.
     * ========================================
     *
     * [직렬화 과정]
     * 1. FileOutputStream: 파일에 바이트를 쓰는 스트림
     * 2. ObjectOutputStream: 객체를 직렬화하여 바이트로 변환
     * 3. writeObject(): 객체를 직렬화하여 파일에 씀
     *
     * [try-with-resources 구문]
     * try (리소스 선언) { } 형태로 사용하면
     * try 블록이 끝날 때 자동으로 리소스가 닫힙니다 (close 호출).
     * 파일 핸들 누수를 방지합니다.
     */
    @Override
    public User save(User user) {
        Path path = resolvePath(user.getId());
        try (
                // 파일 출력 스트림 생성
                FileOutputStream fos = new FileOutputStream(path.toFile());
                // 객체 출력 스트림 생성 (직렬화용)
                ObjectOutputStream oos = new ObjectOutputStream(fos)
        ) {
            // 객체를 직렬화하여 파일에 저장
            oos.writeObject(user);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return user;
    }

    /**
     * ========================================
     * ID로 사용자를 조회합니다. (역직렬화)
     * ========================================
     *
     * [역직렬화 과정]
     * 1. FileInputStream: 파일에서 바이트를 읽는 스트림
     * 2. ObjectInputStream: 바이트를 객체로 역직렬화
     * 3. readObject(): 파일에서 객체를 읽어옴
     * 4. 형변환: Object → User
     */
    @Override
    public Optional<User> findById(UUID id) {
        Path path = resolvePath(id);

        // 파일이 없으면 빈 Optional 반환
        if (Files.notExists(path)) {
            return Optional.empty();
        }

        try (
                // 파일 입력 스트림 생성
                FileInputStream fis = new FileInputStream(path.toFile());
                // 객체 입력 스트림 생성 (역직렬화용)
                ObjectInputStream ois = new ObjectInputStream(fis)
        ) {
            // 파일에서 객체를 읽어 User로 형변환
            return Optional.of((User) ois.readObject());
        } catch (IOException | ClassNotFoundException e) {
            // ClassNotFoundException: 직렬화된 클래스를 찾을 수 없을 때
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        // 모든 사용자를 조회한 후 username으로 필터링
        return findAll().stream()
                .filter(user -> user.getUsername().equals(username))
                .findFirst();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return findAll().stream()
                .filter(user -> user.getEmail().equals(email))
                .findFirst();
    }

    /**
     * ========================================
     * 모든 사용자를 조회합니다.
     * ========================================
     *
     * 디렉토리 내 모든 .ser 파일을 읽어 User 객체로 변환합니다.
     *
     * [Files.list() 메서드]
     * 디렉토리 내의 모든 파일/폴더를 Stream<Path>로 반환합니다.
     */
    @Override
    public List<User> findAll() {
        try {
            return Files.list(DIRECTORY)  // 디렉토리 내 모든 파일 나열
                    .filter(path -> path.toString().endsWith(EXTENSION))  // .ser 파일만 필터
                    .map(path -> {  // 각 파일을 User 객체로 변환
                        try (
                                FileInputStream fis = new FileInputStream(path.toFile());
                                ObjectInputStream ois = new ObjectInputStream(fis)
                        ) {
                            return (User) ois.readObject();
                        } catch (IOException | ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteById(UUID id) {
        Path path = resolvePath(id);
        try {
            // deleteIfExists: 파일이 있으면 삭제, 없으면 아무 것도 안 함
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean existsById(UUID id) {
        Path path = resolvePath(id);
        return Files.exists(path);
    }

    @Override
    public boolean existsByUsername(String username) {
        return findAll().stream()
                .anyMatch(user -> user.getUsername().equals(username));
    }

    @Override
    public boolean existsByEmail(String email) {
        return findAll().stream()
                .anyMatch(user -> user.getEmail().equals(email));
    }
}
