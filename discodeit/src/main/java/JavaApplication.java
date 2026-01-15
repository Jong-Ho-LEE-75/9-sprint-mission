import entity.Channel;
import entity.ChannelType;
import entity.Message;
import entity.User;
import service.ChannelService;
import service.MessageService;
import service.UserService;
import service.jcf.JCFChannelService;
import service.jcf.JCFMessageService;
import service.jcf.JCFUserService;

import java.util.List;
import java.util.UUID;

public class JavaApplication {

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("      🚀 채팅 서비스 통합 테스트 시작      ");
        System.out.println("=========================================");

        // 0. 서비스 초기화 (의존성 주입)
        UserService userService = new JCFUserService();
        ChannelService channelService = new JCFChannelService();
        MessageService messageService = new JCFMessageService(userService, channelService);

        // 테스트를 위해 ID를 저장해둘 변수들
        UUID userId1, userId2, userId3;
        UUID channelId1, channelId2;
        UUID msgId1, msgId2;


        // ====================================================
        // TEST 1. UserService 기능 테스트 (CRUD)
        // ====================================================
        System.out.println("\n[TEST 1] 👤 UserService 테스트");

        // 1-1. 생성 (Create)
        User u1 = userService.create("철수", "cs@test.com", "1234");
        User u2 = userService.create("영희", "yh@test.com", "5678");
        User u3 = userService.create("삭제될사람", "del@test.com", "0000");
        userId1 = u1.getId();
        userId2 = u2.getId();
        userId3 = u3.getId();
        System.out.println("✅ 회원 3명 생성 완료");

        // 1-2. 전체 조회 (FindAll)
        List<User> users = userService.findAll();
        System.out.println("✅ 전체 회원 조회: " + users.size() + "명 (기대값: 3)");

        // 1-3. 단건 조회 (Find)
        User foundUser = userService.find(userId1);
        if (foundUser != null && foundUser.getUserName().equals("철수")) {
            System.out.println("✅ 단건 조회 성공: " + foundUser.getUserName());
        } else {
            System.out.println("❌ 단건 조회 실패");
        }

        // 1-4. 수정 (Update) - 부분 수정 테스트
        // 철수의 이름을 바꾸고, 이메일은 그대로(null), 비번 변경
        userService.update(userId1, "철수(개명)", null, "new_pass");
        User updatedU1 = userService.find(userId1);
        if (updatedU1.getUserName().equals("철수(개명)") && updatedU1.getEmail().equals("cs@test.com")) {
            System.out.println("✅ 회원 정보 수정 성공: 이름 변경됨, 이메일 유지됨");
        } else {
            System.out.println("❌ 회원 정보 수정 실패");
        }

        // 1-5. 삭제 (Delete)
        userService.delete(userId3);
        if (userService.find(userId3) == null) {
            System.out.println("✅ 회원 삭제 성공: 조회되지 않음");
        } else {
            System.out.println("❌ 회원 삭제 실패");
        }


        // ====================================================
        // TEST 2. ChannelService 기능 테스트 (CRUD)
        // ====================================================
        System.out.println("\n[TEST 2] 📺 ChannelService 테스트");

        // 2-1. 생성
        Channel c1 = channelService.create(ChannelType.PUBLIC, "자바방", "자바 공부");
        Channel c2 = channelService.create(ChannelType.PRIVATE, "비밀방", "관계자 외 출입금지");
        channelId1 = c1.getId();
        channelId2 = c2.getId();
        System.out.println("✅ 채널 2개 생성 완료");

        // 2-2. 수정
        channelService.update(channelId1, "자바 마스터방", "설명 변경됨");
        Channel updatedC1 = channelService.find(channelId1);
        System.out.println("✅ 채널 수정 완료: " + updatedC1.getName() + " / " + updatedC1.getDescription());

        // 2-3. 삭제
        channelService.delete(channelId2); // 비밀방 삭제
        if (channelService.find(channelId2) == null) {
            System.out.println("✅ 채널 삭제 성공");
        }


        // ====================================================
        // TEST 3. MessageService 기능 테스트 (CRUD)
        // ====================================================
        System.out.println("\n[TEST 3] 💬 MessageService 테스트 (정상 흐름)");

        // 3-1. 생성 (정상 케이스)
        // 철수(u1)가 자바방(c1)에 메시지 전송
        Message m1 = messageService.create("안녕하세요!", channelId1, userId1);
        // 영희(u2)가 자바방(c1)에 메시지 전송
        Message m2 = messageService.create("반가워요~", channelId1, userId2);
        msgId1 = m1.getId();
        msgId2 = m2.getId();
        System.out.println("✅ 메시지 2건 전송 성공");

        // 3-2. 전체 조회
        System.out.println("✅ 전체 메시지 수: " + messageService.findAll().size() + "개 (기대값: 2)");

        // 3-3. 수정
        messageService.update(msgId1, "안녕하세요! (수정됨)");
        if (messageService.find(msgId1).getContent().contains("(수정됨)")) {
            System.out.println("✅ 메시지 수정 성공: " + messageService.find(msgId1).getContent());
        }

        // 3-4. 삭제
        messageService.delete(msgId2); // 영희 메시지 삭제
        if (messageService.find(msgId2) == null) {
            System.out.println("✅ 메시지 삭제 성공");
            System.out.println("   현재 남은 메시지 수: " + messageService.findAll().size() + "개 (기대값: 1)");
        }


        // ====================================================
        // TEST 4. [핵심] 무결성 검증 및 예외 처리 (Fail Test)
        // ====================================================
        System.out.println("\n[TEST 4] 🛡️ 무결성 및 예외 방어 테스트");

        // 시나리오 A: 존재하지 않는 회원(삭제된 userId3)이 메시지를 보내려고 함
        System.out.print("👉 시나리오 A (삭제된 회원): ");
        try {
            messageService.create("유령입니다..", channelId1, userId3);
            System.out.println("❌ 실패! (예외가 발생했어야 함)");
        } catch (IllegalArgumentException e) {
            System.out.println("✅ 방어 성공! [Reason: " + e.getMessage() + "]");
        }

        // 시나리오 B: 존재하지 않는 채널(삭제된 channelId2)에 메시지를 보내려고 함
        System.out.print("👉 시나리오 B (삭제된 채널): ");
        try {
            messageService.create("이 방 없나요?", channelId2, userId1);
            System.out.println("❌ 실패! (예외가 발생했어야 함)");
        } catch (IllegalArgumentException e) {
            System.out.println("✅ 방어 성공! [Reason: " + e.getMessage() + "]");
        }

        // 시나리오 C: 아예 랜덤한 가짜 UUID 사용
        System.out.print("👉 시나리오 C (가짜 UUID): ");
        try {
            messageService.create("해킹 시도", UUID.randomUUID(), UUID.randomUUID());
            System.out.println("❌ 실패! (예외가 발생했어야 함)");
        } catch (IllegalArgumentException e) {
            System.out.println("✅ 방어 성공! [Reason: " + e.getMessage() + "]");
        }


        // ====================================================
        // 5. 최종 리포트
        // ====================================================
        System.out.println("\n=========================================");
        System.out.println("      🎉 모든 테스트가 종료되었습니다      ");
        System.out.println("   최종 데이터 현황:");
        System.out.println("   - 남은 유저: " + userService.findAll().size() + "명 (철수, 영희)");
        System.out.println("   - 남은 채널: " + channelService.findAll().size() + "개 (자바 마스터방)");
        System.out.println("   - 남은 메시지: " + messageService.findAll().size() + "개 (철수의 메시지)");
        System.out.println("=========================================");
    }
}