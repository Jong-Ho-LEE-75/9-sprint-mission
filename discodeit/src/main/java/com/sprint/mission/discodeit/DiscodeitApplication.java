package com.sprint.mission.discodeit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.sprint.mission.discodeit.dto.request.*;
import com.sprint.mission.discodeit.dto.response.ChannelResponse;
import com.sprint.mission.discodeit.dto.response.MessageResponse;
import com.sprint.mission.discodeit.dto.response.UserResponse;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.service.*;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * ========================================
 * Discodeit 애플리케이션의 메인 클래스
 * ========================================
 *
 * 이 클래스는 Spring Boot 애플리케이션의 시작점(Entry Point)입니다.
 * Discord와 유사한 실시간 메시징 플랫폼으로, 다음과 같은 핵심 기능을 제공합니다:
 *
 * [주요 기능]
 * 1. 사용자 관리 (User Management)
 *    - 회원가입, 로그인, 프로필 이미지 업로드
 *    - 사용자 정보 수정 및 삭제
 *
 * 2. 채널 관리 (Channel Management)
 *    - PUBLIC 채널: 모든 사용자가 접근 가능한 공개 채널
 *    - PRIVATE 채널: 초대된 사용자만 접근 가능한 비공개 채널 (DM)
 *
 * 3. 메시지 관리 (Message Management)
 *    - 텍스트 메시지 작성, 수정, 삭제
 *    - 파일 첨부 기능
 *
 * 4. 상태 관리 (Status Management)
 *    - 읽기 상태 추적 (어디까지 읽었는지)
 *    - 사용자 온라인/오프라인 상태 관리
 *
 * [어노테이션 설명]
 * @SpringBootApplication: 이 어노테이션은 다음 3가지를 한번에 설정합니다:
 *   - @Configuration: 이 클래스가 Spring 설정 클래스임을 표시
 *   - @EnableAutoConfiguration: Spring Boot의 자동 설정 기능 활성화
 *   - @ComponentScan: 현재 패키지와 하위 패키지에서 Spring Bean 자동 검색
 *
 * main 메서드는 애플리케이션을 시작하고 모든 기능을 테스트하는 데모 코드를 포함합니다.
 */
@SpringBootApplication
public class DiscodeitApplication {

	/**
	 * ========================================
	 * 애플리케이션 시작점 (Entry Point)
	 * ========================================
	 *
	 * Java 애플리케이션이 실행될 때 가장 먼저 호출되는 메서드입니다.
	 * Spring Boot 애플리케이션을 시작하고 전체 기능을 테스트합니다.
	 *
	 * @param args 명령행 인수 (Command Line Arguments)
	 *             - 터미널에서 프로그램 실행 시 전달하는 추가 정보
	 *             - 예: java -jar app.jar --server.port=8081
	 */
	public static void main(String[] args) {
		// ========================================
		// 1단계: Spring Boot 애플리케이션 컨텍스트 시작
		// ========================================
		// SpringApplication.run()은 Spring Boot 애플리케이션을 시작하는 핵심 메서드입니다.
		// 이 메서드가 실행되면:
		// 1) 내장 톰캣 서버가 시작됩니다
		// 2) @Component, @Service, @Repository 등이 붙은 클래스들이 Bean으로 등록됩니다
		// 3) 의존성 주입(DI)이 자동으로 이루어집니다
		//
		// ConfigurableApplicationContext는 Spring의 IoC 컨테이너로,
		// 모든 Bean을 관리하고 필요할 때 제공합니다.
		ConfigurableApplicationContext context = SpringApplication.run(DiscodeitApplication.class, args);

		// ========================================
		// 2단계: Spring Context에서 서비스 Bean 조회
		// ========================================
		// context.getBean()을 사용하면 Spring이 관리하는 Bean을 가져올 수 있습니다.
		// 이것이 바로 "의존성 주입(Dependency Injection)"의 핵심입니다.
		//
		// 왜 new UserService()로 직접 생성하지 않나요?
		// → Spring이 Bean을 관리하면 다음과 같은 이점이 있습니다:
		//   1) 싱글톤 보장: 애플리케이션 전체에서 하나의 인스턴스만 사용
		//   2) 의존성 자동 주입: 필요한 다른 Bean들이 자동으로 연결됨
		//   3) 라이프사이클 관리: 생성, 초기화, 소멸을 Spring이 관리
		UserService userService = context.getBean(UserService.class);
		ChannelService channelService = context.getBean(ChannelService.class);
		MessageService messageService = context.getBean(MessageService.class);
		AuthService authService = context.getBean(AuthService.class);
		UserStatusService userStatusService = context.getBean(UserStatusService.class);
		ReadStatusService readStatusService = context.getBean(ReadStatusService.class);
		BinaryContentService binaryContentService = context.getBean(BinaryContentService.class);

		System.out.println("\n========== Discodeit 서비스 전체 기능 테스트 ==========\n");

		// ========================================
		// 테스트 1: User 생성 (프로필 이미지 포함)
		// ========================================
		// 사용자를 생성할 때 프로필 이미지도 함께 업로드할 수 있습니다.
		// BinaryContentCreateRequest는 파일 업로드를 위한 DTO(Data Transfer Object)입니다.
		System.out.println("--- 1. User 생성 테스트 (프로필 이미지 포함) ---");

		// 프로필 이미지 정보를 담은 요청 객체 생성
		// - fileName: 파일 이름
		// - contentType: MIME 타입 (파일 형식을 나타냄)
		// - data: 실제 파일 데이터 (바이트 배열)
		BinaryContentCreateRequest profileImageRequest = new BinaryContentCreateRequest(
				"profile.png",          // 파일명
				"image/png",            // MIME 타입: PNG 이미지
				new byte[]{1, 2, 3, 4, 5}  // 실제로는 이미지 바이너리 데이터
		);

		// 사용자 정보를 담은 요청 객체 생성
		UserCreateRequest userRequest = new UserCreateRequest("woody", "woody@codeit.com", "woody1234");

		// 사용자 생성 서비스 호출
		// userService.create()는 사용자와 프로필 이미지를 함께 저장하고,
		// UserResponse DTO를 반환합니다.
		UserResponse user = userService.create(userRequest, profileImageRequest);
		System.out.println("User 생성: " + user.id());
		System.out.println("  - username: " + user.username());
		System.out.println("  - email: " + user.email());
		System.out.println("  - online: " + user.isOnline());

		// 프로필 이미지 없이 두 번째 사용자 생성
		// profileRequest에 null을 전달하면 프로필 이미지 없이 생성됩니다.
		UserCreateRequest user2Request = new UserCreateRequest("alice", "alice@codeit.com", "alice1234");
		UserResponse user2 = userService.create(user2Request, null);  // null = 프로필 이미지 없음
		System.out.println("User2 생성: " + user2.id());
		System.out.println("  - username: " + user2.username());

		// ========================================
		// 테스트 2: AuthService 로그인 기능
		// ========================================
		// 로그인은 username과 password를 확인하여 사용자를 인증하는 과정입니다.
		System.out.println("\n--- 2. AuthService 로그인 테스트 ---");

		// 로그인 요청 객체 생성
		LoginRequest loginRequest = new LoginRequest("woody", "woody1234");

		// 로그인 시도 - 성공하면 UserResponse 반환
		UserResponse loggedInUser = authService.login(loginRequest);
		System.out.println("로그인 성공: " + loggedInUser.username());
		System.out.println("  - online: " + loggedInUser.isOnline());

		// 잘못된 비밀번호로 로그인 시도 - 예외가 발생해야 함
		// try-catch 블록으로 예외를 처리합니다.
		try {
			LoginRequest wrongLogin = new LoginRequest("woody", "wrongpassword");
			authService.login(wrongLogin);  // 이 라인에서 예외 발생!
		} catch (NoSuchElementException e) {
			// 예외가 발생하면 이 블록이 실행됩니다.
			System.out.println("잘못된 비밀번호 로그인 실패: " + e.getMessage());
		}

		// ========================================
		// 테스트 3: UserStatus (사용자 온라인 상태)
		// ========================================
		// UserStatus는 사용자가 온라인인지 오프라인인지를 추적합니다.
		// 마지막 접속 시간(lastActiveAt)을 기준으로 5분 이내면 온라인으로 판단합니다.
		System.out.println("\n--- 3. UserStatus 테스트 ---");

		// 사용자 ID로 UserStatus 조회
		UserStatus userStatus = userStatusService.findByUserId(user.id());
		System.out.println("UserStatus 조회: " + userStatus.getId());
		System.out.println("  - userId: " + userStatus.getUserId());
		System.out.println("  - isOnline: " + userStatus.isOnline());

		// UserStatus 업데이트 - 마지막 접속 시간을 현재 시간으로 갱신
		// Instant.now()는 현재 시간을 UTC 기준으로 반환합니다.
		UserStatusUpdateRequest statusUpdateRequest = new UserStatusUpdateRequest(Instant.now());
		userStatusService.updateByUserId(user.id(), statusUpdateRequest);
		System.out.println("UserStatus 업데이트 완료 - lastActiveAt 갱신됨");

		// ========================================
		// 테스트 4: PUBLIC 채널 생성
		// ========================================
		// PUBLIC 채널은 모든 사용자가 접근할 수 있는 공개 채널입니다.
		// 채널 이름과 설명을 가집니다.
		System.out.println("\n--- 4. PUBLIC Channel 생성 테스트 ---");

		// 공개 채널 생성 요청
		PublicChannelCreateRequest publicChannelRequest = new PublicChannelCreateRequest("공지", "공지 채널입니다.");
		ChannelResponse publicChannel = channelService.createPublic(publicChannelRequest);
		System.out.println("PUBLIC Channel 생성: " + publicChannel.id());
		System.out.println("  - name: " + publicChannel.name());
		System.out.println("  - type: " + publicChannel.type());

		// PUBLIC 채널 수정 테스트 - 이름과 설명 변경
		ChannelUpdateRequest channelUpdateRequest = new ChannelUpdateRequest("공지사항", "공지사항 채널입니다.");
		ChannelResponse updatedChannel = channelService.update(publicChannel.id(), channelUpdateRequest);
		System.out.println("PUBLIC Channel 수정: " + updatedChannel.name());

		// ========================================
		// 테스트 5: PRIVATE 채널 생성
		// ========================================
		// PRIVATE 채널은 초대된 사용자만 접근할 수 있는 비공개 채널입니다.
		// Discord의 DM(Direct Message)과 유사합니다.
		// 이름과 설명이 없고, 참여자 목록만 있습니다.
		System.out.println("\n--- 5. PRIVATE Channel 생성 테스트 ---");

		// 비공개 채널 생성 요청 - 참여자 ID 목록을 전달
		// List.of()는 불변 리스트를 생성하는 Java 9+ 메서드입니다.
		PrivateChannelCreateRequest privateChannelRequest = new PrivateChannelCreateRequest(
				List.of(user.id(), user2.id())  // woody와 alice를 참여자로 지정
		);
		ChannelResponse privateChannel = channelService.createPrivate(privateChannelRequest);
		System.out.println("PRIVATE Channel 생성: " + privateChannel.id());
		System.out.println("  - type: " + privateChannel.type());
		System.out.println("  - participantIds: " + privateChannel.participantIds());

		// PRIVATE 채널 수정 시도 - 실패해야 함!
		// PRIVATE 채널은 이름과 설명이 없으므로 수정할 수 없습니다.
		try {
			channelService.update(privateChannel.id(), channelUpdateRequest);
			System.out.println("PRIVATE Channel 수정: 실패 - 예외가 발생해야 합니다!");
		} catch (IllegalArgumentException e) {
			System.out.println("PRIVATE Channel 수정 차단됨: " + e.getMessage());
		}

		// ========================================
		// 테스트 6: ReadStatus (메시지 읽기 상태)
		// ========================================
		// ReadStatus는 사용자가 특정 채널의 메시지를 어디까지 읽었는지 추적합니다.
		// PRIVATE 채널 생성 시 각 참여자에 대해 ReadStatus가 자동으로 생성됩니다.
		System.out.println("\n--- 6. ReadStatus 테스트 ---");

		// 사용자의 모든 ReadStatus 조회
		List<ReadStatus> readStatuses = readStatusService.findAllByUserId(user.id());
		System.out.println("User의 ReadStatus 수: " + readStatuses.size());

		// 각 ReadStatus 정보 출력
		for (ReadStatus rs : readStatuses) {
			System.out.println("  - channelId: " + rs.getChannelId() + ", lastReadAt: " + rs.getLastReadAt());
		}

		// ReadStatus 업데이트 - 마지막으로 읽은 시간 갱신
		if (!readStatuses.isEmpty()) {
			ReadStatus firstReadStatus = readStatuses.get(0);
			ReadStatusUpdateRequest rsUpdateRequest = new ReadStatusUpdateRequest(Instant.now());
			readStatusService.update(firstReadStatus.getId(), rsUpdateRequest);
			System.out.println("ReadStatus 업데이트 완료 - lastReadAt 갱신됨");
		}

		// ========================================
		// 테스트 7: Message 생성 (첨부파일 포함)
		// ========================================
		// 메시지는 텍스트 내용과 첨부파일을 포함할 수 있습니다.
		// 첨부파일은 BinaryContent로 저장되고, 메시지는 첨부파일 ID를 참조합니다.
		System.out.println("\n--- 7. Message 생성 테스트 (첨부파일 포함) ---");

		// 첨부파일 정보 생성
		BinaryContentCreateRequest attachmentRequest = new BinaryContentCreateRequest(
				"document.pdf",         // 파일명
				"application/pdf",      // MIME 타입: PDF 문서
				new byte[]{10, 20, 30, 40, 50}  // 실제로는 PDF 바이너리 데이터
		);

		// 메시지 생성 요청
		MessageCreateRequest messageRequest = new MessageCreateRequest(
				"안녕하세요! 첨부파일입니다.",  // 메시지 내용
				publicChannel.id(),           // 메시지를 보낼 채널 ID
				user.id()                     // 메시지 작성자 ID
		);

		// 메시지 생성 - 첨부파일 목록과 함께 전달
		// List.of()로 단일 첨부파일을 리스트로 감쌉니다.
		MessageResponse message = messageService.create(messageRequest, List.of(attachmentRequest));
		System.out.println("Message 생성: " + message.id());
		System.out.println("  - content: " + message.content());
		System.out.println("  - channelId: " + message.channelId());
		System.out.println("  - authorId: " + message.authorId());
		System.out.println("  - attachmentIds: " + message.attachmentIds());

		// 첨부파일 없는 메시지 생성
		MessageCreateRequest message2Request = new MessageCreateRequest(
				"두 번째 메시지입니다.",
				publicChannel.id(),
				user.id()
		);
		MessageResponse message2 = messageService.create(message2Request, null);  // null = 첨부파일 없음
		System.out.println("Message2 생성 (첨부파일 없음): " + message2.id());

		// ========================================
		// 테스트 8: Message 수정
		// ========================================
		System.out.println("\n--- 8. Message 수정 테스트 ---");

		// 메시지 내용 수정 요청
		MessageUpdateRequest messageUpdateRequest = new MessageUpdateRequest("수정된 메시지입니다.");
		MessageResponse updatedMessage = messageService.update(message2.id(), messageUpdateRequest);
		System.out.println("Message 수정: " + updatedMessage.content());

		// ========================================
		// 테스트 9: User 수정 (프로필 이미지 변경)
		// ========================================
		System.out.println("\n--- 9. User 수정 테스트 ---");

		// 새 프로필 이미지 정보
		BinaryContentCreateRequest newProfileRequest = new BinaryContentCreateRequest(
				"new_profile.jpg",
				"image/jpeg",           // JPEG 이미지
				new byte[]{100, 101, 102}
		);

		// 사용자 정보 수정 요청
		UserUpdateRequest userUpdateRequest = new UserUpdateRequest("woody_updated", "woody_new@codeit.com", "newpassword");

		// 사용자 정보 수정 - 기존 프로필 이미지는 삭제되고 새 이미지로 교체됩니다.
		UserResponse updatedUser = userService.update(user.id(), userUpdateRequest, newProfileRequest);
		System.out.println("User 수정: " + updatedUser.username());
		System.out.println("  - email: " + updatedUser.email());

		// ========================================
		// 테스트 10: BinaryContent 조회
		// ========================================
		// 첨부파일을 ID로 조회하여 파일 정보를 확인합니다.
		System.out.println("\n--- 10. BinaryContent 조회 테스트 ---");

		if (!message.attachmentIds().isEmpty()) {
			// 첫 번째 첨부파일 ID로 조회
			BinaryContent attachment = binaryContentService.find(message.attachmentIds().get(0));
			System.out.println("첨부파일 조회: " + attachment.getId());
			System.out.println("  - fileName: " + attachment.getFileName());
			System.out.println("  - contentType: " + attachment.getContentType());
			System.out.println("  - size: " + attachment.getData().length + " bytes");
		}

		// ========================================
		// 테스트 11: 전체 조회
		// ========================================
		System.out.println("\n--- 11. 전체 조회 테스트 ---");

		// 모든 사용자 조회
		List<UserResponse> users = userService.findAll();
		System.out.println("전체 User 수: " + users.size());

		// 특정 사용자가 접근 가능한 채널 조회
		// PUBLIC 채널은 모두, PRIVATE 채널은 참여한 것만 조회됩니다.
		List<ChannelResponse> channels = channelService.findAllByUserId(user.id());
		System.out.println("User가 볼 수 있는 Channel 수: " + channels.size());

		// 특정 채널의 모든 메시지 조회
		List<MessageResponse> messages = messageService.findAllByChannelId(publicChannel.id());
		System.out.println("Channel의 Message 수: " + messages.size());

		// ========================================
		// 테스트 12: 삭제 (Cascading Delete)
		// ========================================
		// 삭제 시 연관된 데이터도 함께 삭제됩니다 (Cascading Delete).
		// 예: 메시지 삭제 → 첨부파일도 삭제
		//     채널 삭제 → 메시지, ReadStatus도 삭제
		//     사용자 삭제 → 프로필 이미지, UserStatus도 삭제
		System.out.println("\n--- 12. 삭제 테스트 (Cascading) ---");

		// Message 삭제 - 첨부파일도 함께 삭제됨
		System.out.println("Message 삭제 전 - Message 수: " + messageService.findAllByChannelId(publicChannel.id()).size());
		messageService.delete(message.id());
		System.out.println("Message 삭제 후 - Message 수: " + messageService.findAllByChannelId(publicChannel.id()).size());

		// PRIVATE Channel 삭제 - 관련 Message, ReadStatus도 함께 삭제됨
		System.out.println("\nPRIVATE Channel 삭제 테스트");
		channelService.delete(privateChannel.id());
		System.out.println("PRIVATE Channel 삭제 완료");

		// User 삭제 - 관련 UserStatus, BinaryContent(프로필)도 함께 삭제됨
		System.out.println("\nUser2 삭제 테스트");
		userService.delete(user2.id());
		System.out.println("User2 삭제 완료");

		// 남은 사용자 수 확인
		List<UserResponse> remainingUsers = userService.findAll();
		System.out.println("남은 User 수: " + remainingUsers.size());

		System.out.println("\n========== 전체 기능 테스트 완료 ==========\n");
	}
}
