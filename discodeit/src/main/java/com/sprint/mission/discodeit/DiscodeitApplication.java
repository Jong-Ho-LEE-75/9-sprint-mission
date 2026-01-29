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
 * Discodeit 애플리케이션의 메인 클래스
 *
 * Discord와 유사한 메시징 플랫폼으로, 다음 기능을 제공합니다:
 * - 사용자 관리 (회원가입, 로그인, 프로필 이미지)
 * - 채널 관리 (공개 채널, 비공개 채널)
 * - 메시지 관리 (메시지 작성, 첨부파일)
 * - 읽기 상태 추적
 * - 사용자 온라인 상태 관리
 *
 * main 메서드는 애플리케이션의 모든 기능을 테스트하는 데모 코드를 포함합니다.
 */
@SpringBootApplication
public class DiscodeitApplication {

	/**
	 * 애플리케이션 시작점
	 * Spring Boot 애플리케이션을 실행하고 전체 기능을 테스트합니다.
	 *
	 * @param args 명령행 인수
	 */
	public static void main(String[] args) {
		// Spring Boot 애플리케이션 컨텍스트 시작
		ConfigurableApplicationContext context = SpringApplication.run(DiscodeitApplication.class, args);

		// Spring Context에서 Bean 조회
		UserService userService = context.getBean(UserService.class);
		ChannelService channelService = context.getBean(ChannelService.class);
		MessageService messageService = context.getBean(MessageService.class);
		AuthService authService = context.getBean(AuthService.class);
		UserStatusService userStatusService = context.getBean(UserStatusService.class);
		ReadStatusService readStatusService = context.getBean(ReadStatusService.class);
		BinaryContentService binaryContentService = context.getBean(BinaryContentService.class);

		System.out.println("\n========== Discodeit 서비스 전체 기능 테스트 ==========\n");

		// ===== 1. User 생성 테스트 (프로필 이미지 포함) =====
		System.out.println("--- 1. User 생성 테스트 (프로필 이미지 포함) ---");
		BinaryContentCreateRequest profileImageRequest = new BinaryContentCreateRequest(
				"profile.png",
				"image/png",
				new byte[]{1, 2, 3, 4, 5}
		);
		UserCreateRequest userRequest = new UserCreateRequest("woody", "woody@codeit.com", "woody1234");
		UserResponse user = userService.create(userRequest, profileImageRequest);
		System.out.println("User 생성: " + user.id());
		System.out.println("  - username: " + user.username());
		System.out.println("  - email: " + user.email());
		System.out.println("  - online: " + user.isOnline());

		// 두 번째 사용자 생성 (프로필 이미지 없이)
		UserCreateRequest user2Request = new UserCreateRequest("alice", "alice@codeit.com", "alice1234");
		UserResponse user2 = userService.create(user2Request, null);
		System.out.println("User2 생성: " + user2.id());
		System.out.println("  - username: " + user2.username());

		// ===== 2. AuthService 로그인 테스트 =====
		System.out.println("\n--- 2. AuthService 로그인 테스트 ---");
		LoginRequest loginRequest = new LoginRequest("woody", "woody1234");
		UserResponse loggedInUser = authService.login(loginRequest);
		System.out.println("로그인 성공: " + loggedInUser.username());
		System.out.println("  - online: " + loggedInUser.isOnline());

		// 잘못된 비밀번호 테스트
		try {
			LoginRequest wrongLogin = new LoginRequest("woody", "wrongpassword");
			authService.login(wrongLogin);
		} catch (NoSuchElementException e) {
			System.out.println("잘못된 비밀번호 로그인 실패: " + e.getMessage());
		}

		// ===== 3. UserStatus 테스트 =====
		System.out.println("\n--- 3. UserStatus 테스트 ---");
		UserStatus userStatus = userStatusService.findByUserId(user.id());
		System.out.println("UserStatus 조회: " + userStatus.getId());
		System.out.println("  - userId: " + userStatus.getUserId());
		System.out.println("  - isOnline: " + userStatus.isOnline());

		// UserStatus 업데이트
		UserStatusUpdateRequest statusUpdateRequest = new UserStatusUpdateRequest(Instant.now());
		userStatusService.updateByUserId(user.id(), statusUpdateRequest);
		System.out.println("UserStatus 업데이트 완료 - lastActiveAt 갱신됨");

		// ===== 4. PUBLIC Channel 생성 테스트 =====
		System.out.println("\n--- 4. PUBLIC Channel 생성 테스트 ---");
		PublicChannelCreateRequest publicChannelRequest = new PublicChannelCreateRequest("공지", "공지 채널입니다.");
		ChannelResponse publicChannel = channelService.createPublic(publicChannelRequest);
		System.out.println("PUBLIC Channel 생성: " + publicChannel.id());
		System.out.println("  - name: " + publicChannel.name());
		System.out.println("  - type: " + publicChannel.type());

		// PUBLIC Channel 수정 테스트
		ChannelUpdateRequest channelUpdateRequest = new ChannelUpdateRequest("공지사항", "공지사항 채널입니다.");
		ChannelResponse updatedChannel = channelService.update(publicChannel.id(), channelUpdateRequest);
		System.out.println("PUBLIC Channel 수정: " + updatedChannel.name());

		// ===== 5. PRIVATE Channel 생성 테스트 =====
		System.out.println("\n--- 5. PRIVATE Channel 생성 테스트 ---");
		PrivateChannelCreateRequest privateChannelRequest = new PrivateChannelCreateRequest(
				List.of(user.id(), user2.id())
		);
		ChannelResponse privateChannel = channelService.createPrivate(privateChannelRequest);
		System.out.println("PRIVATE Channel 생성: " + privateChannel.id());
		System.out.println("  - type: " + privateChannel.type());
		System.out.println("  - participantIds: " + privateChannel.participantIds());

		// PRIVATE Channel 수정 시도 (실패해야 함)
		try {
			channelService.update(privateChannel.id(), channelUpdateRequest);
			System.out.println("PRIVATE Channel 수정: 실패 - 예외가 발생해야 합니다!");
		} catch (IllegalArgumentException e) {
			System.out.println("PRIVATE Channel 수정 차단됨: " + e.getMessage());
		}

		// ===== 6. ReadStatus 테스트 =====
		System.out.println("\n--- 6. ReadStatus 테스트 ---");
		// PRIVATE 채널 생성 시 자동으로 ReadStatus가 생성되어야 함
		List<ReadStatus> readStatuses = readStatusService.findAllByUserId(user.id());
		System.out.println("User의 ReadStatus 수: " + readStatuses.size());
		for (ReadStatus rs : readStatuses) {
			System.out.println("  - channelId: " + rs.getChannelId() + ", lastReadAt: " + rs.getLastReadAt());
		}

		// ReadStatus 업데이트
		if (!readStatuses.isEmpty()) {
			ReadStatus firstReadStatus = readStatuses.get(0);
			ReadStatusUpdateRequest rsUpdateRequest = new ReadStatusUpdateRequest(Instant.now());
			readStatusService.update(firstReadStatus.getId(), rsUpdateRequest);
			System.out.println("ReadStatus 업데이트 완료 - lastReadAt 갱신됨");
		}

		// ===== 7. Message 생성 테스트 (첨부파일 포함) =====
		System.out.println("\n--- 7. Message 생성 테스트 (첨부파일 포함) ---");
		BinaryContentCreateRequest attachmentRequest = new BinaryContentCreateRequest(
				"document.pdf",
				"application/pdf",
				new byte[]{10, 20, 30, 40, 50}
		);
		MessageCreateRequest messageRequest = new MessageCreateRequest(
				"안녕하세요! 첨부파일입니다.",
				publicChannel.id(),
				user.id()
		);
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
		MessageResponse message2 = messageService.create(message2Request, null);
		System.out.println("Message2 생성 (첨부파일 없음): " + message2.id());

		// ===== 8. Message 수정 테스트 =====
		System.out.println("\n--- 8. Message 수정 테스트 ---");
		MessageUpdateRequest messageUpdateRequest = new MessageUpdateRequest("수정된 메시지입니다.");
		MessageResponse updatedMessage = messageService.update(message2.id(), messageUpdateRequest);
		System.out.println("Message 수정: " + updatedMessage.content());

		// ===== 9. User 수정 테스트 =====
		System.out.println("\n--- 9. User 수정 테스트 ---");
		BinaryContentCreateRequest newProfileRequest = new BinaryContentCreateRequest(
				"new_profile.jpg",
				"image/jpeg",
				new byte[]{100, 101, 102}
		);
		UserUpdateRequest userUpdateRequest = new UserUpdateRequest("woody_updated", "woody_new@codeit.com", "newpassword");
		UserResponse updatedUser = userService.update(user.id(), userUpdateRequest, newProfileRequest);
		System.out.println("User 수정: " + updatedUser.username());
		System.out.println("  - email: " + updatedUser.email());

		// ===== 10. BinaryContent 조회 테스트 =====
		System.out.println("\n--- 10. BinaryContent 조회 테스트 ---");
		if (!message.attachmentIds().isEmpty()) {
			BinaryContent attachment = binaryContentService.find(message.attachmentIds().get(0));
			System.out.println("첨부파일 조회: " + attachment.getId());
			System.out.println("  - fileName: " + attachment.getFileName());
			System.out.println("  - contentType: " + attachment.getContentType());
			System.out.println("  - size: " + attachment.getData().length + " bytes");
		}

		// ===== 11. 조회 테스트 =====
		System.out.println("\n--- 11. 전체 조회 테스트 ---");
		List<UserResponse> users = userService.findAll();
		System.out.println("전체 User 수: " + users.size());

		List<ChannelResponse> channels = channelService.findAllByUserId(user.id());
		System.out.println("User가 볼 수 있는 Channel 수: " + channels.size());

		List<MessageResponse> messages = messageService.findAllByChannelId(publicChannel.id());
		System.out.println("Channel의 Message 수: " + messages.size());

		// ===== 12. 삭제 테스트 (Cascading) =====
		System.out.println("\n--- 12. 삭제 테스트 (Cascading) ---");

		// Message 삭제 (첨부파일도 함께 삭제)
		System.out.println("Message 삭제 전 - Message 수: " + messageService.findAllByChannelId(publicChannel.id()).size());
		messageService.delete(message.id());
		System.out.println("Message 삭제 후 - Message 수: " + messageService.findAllByChannelId(publicChannel.id()).size());

		// Channel 삭제 (관련 Message, ReadStatus 함께 삭제)
		System.out.println("\nPRIVATE Channel 삭제 테스트");
		channelService.delete(privateChannel.id());
		System.out.println("PRIVATE Channel 삭제 완료");

		// User 삭제 테스트 (관련 UserStatus, BinaryContent 함께 삭제)
		System.out.println("\nUser2 삭제 테스트");
		userService.delete(user2.id());
		System.out.println("User2 삭제 완료");
		List<UserResponse> remainingUsers = userService.findAll();
		System.out.println("남은 User 수: " + remainingUsers.size());

		System.out.println("\n========== 전체 기능 테스트 완료 ==========\n");
	}
}
