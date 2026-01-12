import entity.Channel;
import entity.Message;
import entity.User;
import service.ChannelService;
import service.MessageService;
import service.UserService;
import service.jcf.JCFChannelService;
import service.jcf.JCFMessageService;
import service.jcf.JCFUserService;

public class JavaApplication {
    public static void main(String[] args) {
        System.out.println("=== DiscodeIt 서비스 테스트 시작 ===\n");

        // 서비스 초기화
        UserService userService = new JCFUserService();
        ChannelService channelService = new JCFChannelService();
        MessageService messageService = new JCFMessageService();

        // User 테스트
        System.out.println("【 사용자(User) 테스트 】");
        testUserService(userService);

        // Channel 테스트
        System.out.println("\n【 채널(Channel) 테스트 】");
        testChannelService(channelService);

        // Message 테스트
        System.out.println("\n【 메시지(Message) 테스트 】");
        testMessageService(messageService, userService, channelService);

        System.out.println("\n=== 모든 테스트 완료 ===");
    }

    private static void testUserService(UserService userService) {
        // 1. 등록
        System.out.println("\n1️⃣ 사용자 등록");
        User user1 = new User("john_doe", "john@example.com", "John");
        User user2 = new User("jane_smith", "jane@example.com", "Jane");
        userService.create(user1);
        userService.create(user2);
        System.out.println("  ✓ 등록된 사용자1: " + user1);
        System.out.println("  ✓ 등록된 사용자2: " + user2);

        // 2. 조회 (단건)
        System.out.println("\n2️⃣ 사용자 단건 조회");
        User foundUser = userService.findById(user1.getId());
        System.out.println("  ✓ 조회된 사용자: " + foundUser);

        // 3. 조회 (다건)
        System.out.println("\n3️⃣ 사용자 전체 조회");
        System.out.println("  ✓ 전체 사용자 수: " + userService.findAll().size() + "명");
        userService.findAll().forEach(user -> System.out.println("    - " + user));

        // 4. 수정
        System.out.println("\n4️⃣ 사용자 정보 수정");
        userService.update(user1.getId(), "john_updated", "john_new@example.com", "Johnny");
        System.out.println("  ✓ 사용자1 정보 수정 완료");

        // 5. 수정된 데이터 조회
        System.out.println("\n5️⃣ 수정된 사용자 조회");
        User updatedUser = userService.findById(user1.getId());
        System.out.println("  ✓ 수정된 사용자: " + updatedUser);

        // 6. 삭제
        System.out.println("\n6️⃣ 사용자 삭제");
        userService.delete(user2.getId());
        System.out.println("  ✓ 사용자2 삭제 완료");

        // 7. 삭제 확인
        System.out.println("\n7️⃣ 삭제 확인");
        User deletedUser = userService.findById(user2.getId());
        System.out.println("  ✓ 삭제된 사용자 조회 결과: " + (deletedUser == null ? "없음 (삭제됨)" : deletedUser));
        System.out.println("  ✓ 남은 사용자 수: " + userService.findAll().size() + "명");
    }

    private static void testChannelService(ChannelService channelService) {
        // 1. 등록
        System.out.println("\n1️⃣ 채널 등록");
        Channel channel1 = new Channel("general", "일반 대화 채널", "TEXT");
        Channel channel2 = new Channel("voice-chat", "음성 채팅 채널", "VOICE");
        channelService.create(channel1);
        channelService.create(channel2);
        System.out.println("  ✓ 등록된 채널1: " + channel1);
        System.out.println("  ✓ 등록된 채널2: " + channel2);

        // 2. 조회 (단건)
        System.out.println("\n2️⃣ 채널 단건 조회");
        Channel foundChannel = channelService.findById(channel1.getId());
        System.out.println("  ✓ 조회된 채널: " + foundChannel);

        // 3. 조회 (다건)
        System.out.println("\n3️⃣ 채널 전체 조회");
        System.out.println("  ✓ 전체 채널 수: " + channelService.findAll().size() + "개");
        channelService.findAll().forEach(channel -> System.out.println("    - " + channel));

        // 4. 수정
        System.out.println("\n4️⃣ 채널 정보 수정");
        channelService.update(channel1.getId(), "general-updated", "업데이트된 일반 채널", "TEXT");
        System.out.println("  ✓ 채널1 정보 수정 완료");

        // 5. 수정된 데이터 조회
        System.out.println("\n5️⃣ 수정된 채널 조회");
        Channel updatedChannel = channelService.findById(channel1.getId());
        System.out.println("  ✓ 수정된 채널: " + updatedChannel);

        // 6. 삭제
        System.out.println("\n6️⃣ 채널 삭제");
        channelService.delete(channel2.getId());
        System.out.println("  ✓ 채널2 삭제 완료");

        // 7. 삭제 확인
        System.out.println("\n7️⃣ 삭제 확인");
        Channel deletedChannel = channelService.findById(channel2.getId());
        System.out.println("  ✓ 삭제된 채널 조회 결과: " + (deletedChannel == null ? "없음 (삭제됨)" : deletedChannel));
        System.out.println("  ✓ 남은 채널 수: " + channelService.findAll().size() + "개");
    }

    private static void testMessageService(MessageService messageService, UserService userService, ChannelService channelService) {
        // 테스트용 User와 Channel 생성
        User testUser = new User("msg_user", "msg@example.com", "MsgUser");
        Channel testChannel = new Channel("test-channel", "테스트 채널", "TEXT");
        userService.create(testUser);
        channelService.create(testChannel);

        // 1. 등록
        System.out.println("\n1️⃣ 메시지 등록");
        Message message1 = new Message("안녕하세요!", testUser.getId(), testChannel.getId());
        Message message2 = new Message("반갑습니다.", testUser.getId(), testChannel.getId());
        messageService.create(message1);
        messageService.create(message2);

        System.out.println("  ✓ 등록된 메시지1: " + formatMessage(message1, userService, channelService));
        System.out.println("  ✓ 등록된 메시지2: " + formatMessage(message2, userService, channelService));

        // 2. 조회 (단건)
        System.out.println("\n2️⃣ 메시지 단건 조회");
        Message foundMessage = messageService.findById(message1.getId());
        System.out.println("  ✓ 조회된 메시지: " + formatMessage(foundMessage, userService, channelService));

        // 3. 조회 (다건)
        System.out.println("\n3️⃣ 메시지 전체 조회");
        System.out.println("  ✓ 전체 메시지 수: " + messageService.findAll().size() + "개");
        messageService.findAll().forEach(message ->
                System.out.println("    - " + formatMessage(message, userService, channelService)));

        // 4. 수정
        System.out.println("\n4️⃣ 메시지 내용 수정");
        messageService.update(message1.getId(), "수정된 메시지입니다!", testUser.getId(), testChannel.getId());
        System.out.println("  ✓ 메시지1 내용 수정 완료");

        // 5. 수정된 데이터 조회
        System.out.println("\n5️⃣ 수정된 메시지 조회");
        Message updatedMessage = messageService.findById(message1.getId());
        System.out.println("  ✓ 수정된 메시지: " + formatMessage(updatedMessage, userService, channelService));

        // 6. 삭제
        System.out.println("\n6️⃣ 메시지 삭제");
        messageService.delete(message2.getId());
        System.out.println("  ✓ 메시지2 삭제 완료");

        // 7. 삭제 확인
        System.out.println("\n7️⃣ 삭제 확인");
        Message deletedMessage = messageService.findById(message2.getId());
        System.out.println("  ✓ 삭제된 메시지 조회 결과: " + (deletedMessage == null ? "없음 (삭제됨)" : formatMessage(deletedMessage, userService, channelService)));
        System.out.println("  ✓ 남은 메시지 수: " + messageService.findAll().size() + "개");
    }

    // 메시지를 예쁘게 포맷팅하는 헬퍼 메소드
    private static String formatMessage(Message message, UserService userService, ChannelService channelService) {
        if (message == null) {
            return null;
        }

        User user = userService.findById(message.getUserId());
        Channel channel = channelService.findById(message.getChannelId());

        String userName = user != null ? user.getNickname() : "알 수 없음";
        String channelName = channel != null ? channel.getName() : "알 수 없음";

        return String.format("메시지 [작성자: %s, 채널: %s, 내용: \"%s\"]",
                userName, channelName, message.getContent());
    }
}