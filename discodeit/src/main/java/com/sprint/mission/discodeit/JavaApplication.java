package com.sprint.mission.discodeit;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.repository.file.FileChannelRepository;
import com.sprint.mission.discodeit.repository.file.FileMessageRepository;
import com.sprint.mission.discodeit.repository.file.FileUserRepository;
import com.sprint.mission.discodeit.repository.jcf.JCFChannelRepository;
import com.sprint.mission.discodeit.repository.jcf.JCFMessageRepository;
import com.sprint.mission.discodeit.repository.jcf.JCFUserRepository;
import com.sprint.mission.discodeit.service.ChannelService;
import com.sprint.mission.discodeit.service.MessageService;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.service.basic.BasicChannelService;
import com.sprint.mission.discodeit.service.basic.BasicMessageService;
import com.sprint.mission.discodeit.service.basic.BasicUserService;
import com.sprint.mission.discodeit.service.file.FileChannelService;
import com.sprint.mission.discodeit.service.file.FileMessageService;
import com.sprint.mission.discodeit.service.file.FileUserService;
import com.sprint.mission.discodeit.service.jcf.JCFChannelService;
import com.sprint.mission.discodeit.service.jcf.JCFMessageService;
import com.sprint.mission.discodeit.service.jcf.JCFUserService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Discodeit 애플리케이션 메인 클래스
 *
 * 심화 요구사항: 관심사 분리를 통한 레이어 간 의존성 주입
 * - Basic*Service 구현체를 활용하여 테스트
 * - JCF*Repository 구현체를 활용하여 테스트
 * - File*Repository 구현체를 활용하여 테스트
 */
public class JavaApplication {
    private static PrintWriter fileWriter;
    private static int testPassCount = 0;
    private static int testFailCount = 0;

    // 결과 파일 저장 경로 (com.sprint.mission.discodeit 폴더 내)
    private static final String RESULTS_DIR = "discodeit/src/main/java/com/sprint/mission/discodeit/test-results";

    // ==================== 유틸리티 메서드 ====================

    private static void log(String message) {
        System.out.println(message);
        if (fileWriter != null) {
            fileWriter.println(message);
        }
    }

    private static void logSuccess(String testName) {
        testPassCount++;
        log("  [PASS] " + testName);
    }

    private static void logFail(String testName, String reason) {
        testFailCount++;
        log("  [FAIL] " + testName + " - " + reason);
    }

    private static void logSection(String title) {
        log("\n" + "=".repeat(60));
        log("  " + title);
        log("=".repeat(60));
    }

    // ==================== 심화 요구사항 템플릿 메서드 ====================

    /**
     * 테스트용 사용자 생성
     */
    static User setupUser(UserService userService) {
        User user = userService.create("woody", "woody@codeit.com", "woody1234");
        return user;
    }

    /**
     * 테스트용 채널 생성
     */
    static Channel setupChannel(ChannelService channelService) {
        Channel channel = channelService.create(ChannelType.PUBLIC, "공지", "공지 채널입니다.");
        return channel;
    }

    /**
     * 메시지 생성 테스트
     */
    static void messageCreateTest(MessageService messageService, Channel channel, User author) {
        Message message = messageService.create("안녕하세요.", channel.getId(), author.getId());
        log("메시지 생성: " + message.getId());
        log("  - 내용: " + message.getContent());
        log("  - 채널ID: " + message.getChannelId());
        log("  - 작성자ID: " + message.getAuthorId());
    }

    // ==================== CRUD 테스트 메서드 ====================

    static void userCRUDTest(UserService userService, String serviceName) {
        log("\n--- " + serviceName + " User CRUD 테스트 ---");

        // 생성
        User user = userService.create("testUser", "test@codeit.com", "pass1234");
        if (user != null && user.getId() != null) {
            logSuccess("User 생성: " + user.getId());
        } else {
            logFail("User 생성", "생성 실패");
            return;
        }

        // 조회 (단건)
        User foundUser = userService.find(user.getId());
        if (foundUser != null && foundUser.getUsername().equals("testUser")) {
            logSuccess("User 단건 조회: " + foundUser.getUsername());
        } else {
            logFail("User 단건 조회", "조회 실패");
        }

        // 조회 (전체)
        List<User> users = userService.findAll();
        if (users != null && !users.isEmpty()) {
            logSuccess("User 전체 조회: " + users.size() + "건");
        } else {
            logFail("User 전체 조회", "조회 결과 없음");
        }

        // 수정
        User updatedUser = userService.update(user.getId(), "updatedUser", null, "newPass");
        if (updatedUser != null && updatedUser.getUsername().equals("updatedUser")) {
            logSuccess("User 수정: " + updatedUser.getUsername());
        } else {
            logFail("User 수정", "수정 실패");
        }

        // 삭제
        int before = userService.findAll().size();
        userService.delete(user.getId());
        int after = userService.findAll().size();
        if (after < before) {
            logSuccess("User 삭제: " + before + " -> " + after);
        } else {
            logFail("User 삭제", "삭제 실패");
        }
    }

    static void channelCRUDTest(ChannelService channelService, String serviceName) {
        log("\n--- " + serviceName + " Channel CRUD 테스트 ---");

        // 생성 (PUBLIC)
        Channel publicChannel = channelService.create(ChannelType.PUBLIC, "공개채널", "공개 채널입니다.");
        if (publicChannel != null && publicChannel.getType() == ChannelType.PUBLIC) {
            logSuccess("PUBLIC Channel 생성: " + publicChannel.getName());
        } else {
            logFail("PUBLIC Channel 생성", "생성 실패");
        }

        // 생성 (PRIVATE)
        Channel privateChannel = channelService.create(ChannelType.PRIVATE, "비공개채널", "비공개 채널입니다.");
        if (privateChannel != null && privateChannel.getType() == ChannelType.PRIVATE) {
            logSuccess("PRIVATE Channel 생성: " + privateChannel.getName());
        } else {
            logFail("PRIVATE Channel 생성", "생성 실패");
        }

        // 조회 (단건)
        Channel foundChannel = channelService.find(publicChannel.getId());
        if (foundChannel != null) {
            logSuccess("Channel 단건 조회: " + foundChannel.getName());
        } else {
            logFail("Channel 단건 조회", "조회 실패");
        }

        // 조회 (전체)
        List<Channel> channels = channelService.findAll();
        if (channels != null && channels.size() >= 2) {
            logSuccess("Channel 전체 조회: " + channels.size() + "건");
        } else {
            logFail("Channel 전체 조회", "조회 결과 부족");
        }

        // 수정
        Channel updatedChannel = channelService.update(publicChannel.getId(), "수정된채널", "설명도 수정됨");
        if (updatedChannel != null && updatedChannel.getName().equals("수정된채널")) {
            logSuccess("Channel 수정: " + updatedChannel.getName());
        } else {
            logFail("Channel 수정", "수정 실패");
        }

        // 삭제
        int before = channelService.findAll().size();
        channelService.delete(publicChannel.getId());
        channelService.delete(privateChannel.getId());
        int after = channelService.findAll().size();
        if (after < before) {
            logSuccess("Channel 삭제: " + before + " -> " + after);
        } else {
            logFail("Channel 삭제", "삭제 실패");
        }
    }

    static void messageCRUDTest(MessageService messageService, UserService userService,
                                ChannelService channelService, String serviceName) {
        log("\n--- " + serviceName + " Message CRUD 테스트 ---");

        // 테스트용 User, Channel 생성
        User author = userService.create("msgAuthor", "author@codeit.com", "author123");
        Channel channel = channelService.create(ChannelType.PUBLIC, "메시지테스트채널", "테스트용");

        // 생성
        Message message = messageService.create("안녕하세요!", channel.getId(), author.getId());
        if (message != null && message.getContent().equals("안녕하세요!")) {
            logSuccess("Message 생성: " + message.getContent());
        } else {
            logFail("Message 생성", "생성 실패");
        }

        // 조회 (단건)
        Message foundMessage = messageService.find(message.getId());
        if (foundMessage != null) {
            logSuccess("Message 단건 조회: " + foundMessage.getContent());
        } else {
            logFail("Message 단건 조회", "조회 실패");
        }

        // 조회 (전체)
        List<Message> messages = messageService.findAll();
        if (messages != null && !messages.isEmpty()) {
            logSuccess("Message 전체 조회: " + messages.size() + "건");
        } else {
            logFail("Message 전체 조회", "조회 결과 없음");
        }

        // 수정
        Message updatedMessage = messageService.update(message.getId(), "수정된 메시지입니다.");
        if (updatedMessage != null && updatedMessage.getContent().equals("수정된 메시지입니다.")) {
            logSuccess("Message 수정: " + updatedMessage.getContent());
        } else {
            logFail("Message 수정", "수정 실패");
        }

        // 삭제
        int before = messageService.findAll().size();
        messageService.delete(message.getId());
        int after = messageService.findAll().size();
        if (after < before) {
            logSuccess("Message 삭제: " + before + " -> " + after);
        } else {
            logFail("Message 삭제", "삭제 실패");
        }

        // 테스트 데이터 정리
        userService.delete(author.getId());
        channelService.delete(channel.getId());
    }

    // ==================== 서비스 구현체별 테스트 ====================

    /**
     * Basic 서비스 + Repository 테스트 (통합 메서드)
     * Repository를 주입받아 하나의 코드로 JCF/File 테스트 수행
     */
    static void testBasicWithRepository(UserRepository userRepo, ChannelRepository channelRepo,
                                        MessageRepository messageRepo, String repoType, String description) {
        logSection("Basic 서비스 + " + repoType + " Repository 테스트");
        log("(" + description + ", Repository 패턴 적용)");

        // Basic 서비스 초기화 (Repository 주입)
        UserService userService = new BasicUserService(userRepo);
        ChannelService channelService = new BasicChannelService(channelRepo);
        MessageService messageService = new BasicMessageService(messageRepo);

        // CRUD 테스트
        userCRUDTest(userService, "Basic+" + repoType);
        channelCRUDTest(channelService, "Basic+" + repoType);

        // 심화 요구사항 템플릿 테스트
        log("\n--- Basic+" + repoType + " 템플릿 테스트 ---");
        User user = setupUser(userService);
        Channel channel = setupChannel(channelService);
        messageCreateTest(messageService, channel, user);
        logSuccess("템플릿 테스트 완료");
    }

    /**
     * 3. JCF 서비스 테스트 (비교용)
     */
    static void testJCFServices() {
        logSection("JCF 서비스 테스트 (비교용)");
        log("(메모리 기반, Repository 미사용)");

        UserService userService = new JCFUserService();
        ChannelService channelService = new JCFChannelService();
        MessageService messageService = new JCFMessageService(userService, channelService);

        userCRUDTest(userService, "JCF");
        channelCRUDTest(channelService, "JCF");
        messageCRUDTest(messageService, userService, channelService, "JCF");
    }

    /**
     * 4. File 서비스 테스트 (비교용)
     */
    static void testFileServices() {
        logSection("File 서비스 테스트 (비교용)");
        log("(파일 기반, Repository 미사용)");

        UserService userService = new FileUserService();
        ChannelService channelService = new FileChannelService();
        MessageService messageService = new FileMessageService(userService, channelService);

        userCRUDTest(userService, "File");
        channelCRUDTest(channelService, "File");
        messageCRUDTest(messageService, userService, channelService, "File");
    }

    /**
     * 서비스 구현체 비교 출력
     */
    static void printComparison() {
        logSection("서비스 구현체 비교 분석");

        log("\n[1] JCF 서비스 vs Basic 서비스 + JCF Repository");
        log("   공통점: 메모리 기반, 애플리케이션 종료 시 데이터 소멸");
        log("   차이점:");
        log("   - JCF 서비스: 서비스 내부에서 HashMap으로 직접 데이터 관리");
        log("   - Basic+JCF: Repository 인터페이스를 통해 데이터 접근 (계층 분리)");

        log("\n[2] File 서비스 vs Basic 서비스 + File Repository");
        log("   공통점: 파일 기반, 데이터 영속성 보장");
        log("   차이점:");
        log("   - File 서비스: 서비스 내부에서 파일 I/O 직접 처리");
        log("   - Basic+File: Repository 인터페이스를 통해 파일 I/O 위임 (계층 분리)");

        log("\n[3] Basic 서비스의 장점");
        log("   - 단일 책임 원칙(SRP) 준수: 비즈니스 로직과 저장 로직 분리");
        log("   - 의존성 역전 원칙(DIP): Repository 인터페이스에 의존");
        log("   - 테스트 용이성: Repository Mock 객체 주입 가능");
        log("   - 유연한 저장소 교체: JCF -> File -> DB 전환 용이");
    }

    // ==================== 메인 메서드 ====================

    public static void main(String[] args) {
        // 결과 파일 저장 디렉토리 생성
        File resultsDir = new File(RESULTS_DIR);
        if (!resultsDir.exists()) {
            resultsDir.mkdirs();
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String resultFileName = RESULTS_DIR + "/test-result-" + timestamp + ".txt";

        try (PrintWriter pw = new PrintWriter(new FileWriter(resultFileName))) {
            fileWriter = pw;

            log("╔════════════════════════════════════════════════════════════════╗");
            log("║        Discodeit 심화 요구사항 테스트 리포트                          ║");
            log("║        관심사 분리를 통한 레이어 간 의존성 주입                          ║");
            log("╠════════════════════════════════════════════════════════════════╣");
            log("║  실행 시간: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "                                 ║");
            log("╚════════════════════════════════════════════════════════════════╝");

            // 1. Basic 서비스 + JCF Repository 테스트
            testBasicWithRepository(
                    new JCFUserRepository(),
                    new JCFChannelRepository(),
                    new JCFMessageRepository(),
                    "JCF", "메모리 기반"
            );

            // 2. Basic 서비스 + File Repository 테스트
            testBasicWithRepository(
                    new FileUserRepository(),
                    new FileChannelRepository(),
                    new FileMessageRepository(),
                    "File", "파일 기반"
            );

            // 3. JCF 서비스 테스트 (비교용)
            testJCFServices();

            // 4. File 서비스 테스트 (비교용)
            testFileServices();

            // 5. 서비스 구현체 비교 분석
            printComparison();

            // 최종 결과 출력
            log("\n╔════════════════════════════════════════════════════════════════╗");
            log("║                      테스트 결과 요약                              ║");
            log("╠════════════════════════════════════════════════════════════════╣");
            log("║  총 테스트: " + String.format("%-3d", testPassCount + testFailCount) + "건                                                  ║");
            log("║  성공: " + String.format("%-3d", testPassCount) + "건                                                     ║");
            log("║  실패: " + String.format("%-3d", testFailCount) + "건                                                     ║");
            log("║  성공률: " + String.format("%-6.1f", testPassCount * 100.0 / (testPassCount + testFailCount)) + "%                                               ║");
            log("╚════════════════════════════════════════════════════════════════╝");
            log("   결과 파일: " + resultFileName);

        }
        catch (IOException e) {
            System.err.println("파일 저장 중 오류 발생: " + e.getMessage());
        }
    }
}
