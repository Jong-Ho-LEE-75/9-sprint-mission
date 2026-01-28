# Discodeit 프로젝트 완전 가이드 (초보자용)

> Discord 클론 백엔드 프로젝트를 처음부터 끝까지 이해할 수 있는 상세 가이드입니다.

---

## 목차

1. [프로젝트 소개](#1-프로젝트-소개)
2. [프로젝트 구조 한눈에 보기](#2-프로젝트-구조-한눈에-보기)
3. [Entity (엔티티) - 데이터 모델](#3-entity-엔티티---데이터-모델)
4. [Repository (레포지토리) - 데이터 저장소](#4-repository-레포지토리---데이터-저장소)
5. [Service (서비스) - 비즈니스 로직](#5-service-서비스---비즈니스-로직)
6. [왜 이렇게 코딩했나? - 설계 원칙](#6-왜-이렇게-코딩했나---설계-원칙)
7. [코드 실행 흐름 따라가기](#7-코드-실행-흐름-따라가기)
8. [핵심 개념 정리](#8-핵심-개념-정리)

---

## 1. 프로젝트 소개

### 1.1 Discodeit이란?

**Discodeit**는 Discord(디스코드)를 모방한 채팅 애플리케이션의 백엔드 시스템입니다.

실제 Discord처럼:
- **사용자(User)**: 가입하고, 로그인하고, 정보를 수정할 수 있음
- **채널(Channel)**: 대화방을 만들고, 참여할 수 있음
- **메시지(Message)**: 채널에서 대화를 주고받을 수 있음

### 1.2 이 프로젝트의 학습 목표

이 프로젝트는 단순한 CRUD 구현을 넘어서 **좋은 소프트웨어 설계**를 배우기 위한 교육용 프로젝트입니다.

배우게 되는 것들:
- **계층 분리**: Entity, Repository, Service를 나누는 이유
- **인터페이스 기반 설계**: 왜 인터페이스를 먼저 만들까?
- **의존성 주입(DI)**: 왜 new로 직접 만들지 않고 주입받을까?
- **Repository 패턴**: 데이터 저장 방식을 쉽게 바꾸는 방법

---

## 2. 프로젝트 구조 한눈에 보기

```
discodeit/src/main/java/com/sprint/mission/discodeit/
│
├── JavaApplication.java          ← 프로그램 시작점 (main 메서드)
│
├── entity/                       ← 데이터 모델 (DB 테이블과 유사)
│   ├── BaseEntity.java          ← 모든 엔티티의 부모 클래스
│   ├── User.java                ← 사용자 정보
│   ├── Channel.java             ← 채널 정보
│   ├── Message.java             ← 메시지 정보
│   └── ChannelType.java         ← 채널 종류 (PUBLIC/PRIVATE)
│
├── repository/                   ← 데이터 저장/조회 담당
│   ├── UserRepository.java      ← 인터페이스 (계약서)
│   ├── jcf/                     ← 메모리 저장 방식
│   │   └── JCFUserRepository.java
│   └── file/                    ← 파일 저장 방식
│       └── FileUserRepository.java
│
├── service/                      ← 비즈니스 로직 담당
│   ├── UserService.java         ← 인터페이스 (계약서)
│   ├── basic/                   ← 권장되는 구현 방식
│   │   └── BasicUserService.java
│   ├── jcf/                     ← 비교용 (Repository 미사용)
│   │   └── JCFUserService.java
│   └── file/                    ← 비교용 (Repository 미사용)
│       └── FileUserService.java
│
└── file-data-map/               ← 파일로 저장된 데이터들
    ├── User/
    ├── Channel/
    └── Message/
```

### 2.1 왜 이렇게 폴더를 나눌까?

**관심사의 분리(Separation of Concerns)** 원칙 때문입니다.

| 폴더 | 담당하는 일 | 비유 |
|------|------------|------|
| entity | 데이터가 어떻게 생겼는지 정의 | 설계도 |
| repository | 데이터를 저장하고 꺼내오기 | 창고 관리인 |
| service | 비즈니스 규칙 적용 | 업무 담당자 |

각자 맡은 일만 하면 됩니다. 창고 관리인이 업무 규칙을 몰라도 되고, 업무 담당자가 창고가 어디 있는지 몰라도 됩니다.

---

## 3. Entity (엔티티) - 데이터 모델

Entity는 **"데이터가 어떻게 생겼는지"**를 정의합니다. 데이터베이스의 테이블과 비슷한 개념입니다.

### 3.1 BaseEntity - 모든 엔티티의 부모

```java
public class BaseEntity implements Serializable {
    protected UUID id;           // 고유 식별자 (주민등록번호 같은 것)
    protected long createdAt;    // 언제 만들어졌는지
    protected long updatedAt;    // 언제 수정되었는지

    public BaseEntity() {
        this.id = UUID.randomUUID();  // 자동으로 고유한 ID 생성
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    protected void updateTimeStamp() {
        this.updatedAt = System.currentTimeMillis();  // 수정 시간 갱신
    }
}
```

**왜 BaseEntity를 만들었나?**

User, Channel, Message 모두 공통으로 필요한 것들이 있습니다:
- 고유한 ID
- 생성 시간
- 수정 시간

이걸 매번 복사-붙여넣기 하면:
- 코드 중복 발생
- 나중에 수정할 때 3곳 다 고쳐야 함
- 실수로 한 곳만 고치면 버그 발생

그래서 **상속**을 사용해 공통 기능을 한 곳에 모았습니다.

**Serializable이 뭔가요?**

```java
implements Serializable
```

이건 "이 객체를 파일로 저장할 수 있다"는 뜻입니다. 나중에 File Repository에서 객체를 `.ser` 파일로 저장할 때 필요합니다.

### 3.2 User - 사용자

```java
public class User extends BaseEntity {
    private String userName;   // 닉네임
    private String email;      // 이메일
    private String password;   // 비밀번호

    // 생성자: User를 만들 때 필요한 정보
    public User(String userName, String email, String password) {
        super();  // 부모(BaseEntity)의 생성자 호출 → ID 자동 생성
        this.userName = userName;
        this.email = email;
        this.password = password;
    }

    // Getter: 정보를 읽을 때 사용
    public String getUserName() { return userName; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }

    // 수정 메서드: 정보를 변경할 때 사용
    public void update(String userName, String email, String password) {
        if (userName != null) this.userName = userName;
        if (email != null) this.email = email;
        if (password != null) this.password = password;
        updateTimeStamp();  // 부모의 메서드 호출 → 수정 시간 갱신
    }
}
```

**왜 update 메서드에서 null 체크를 하나요?**

```java
if (userName != null) this.userName = userName;
```

이유: **부분 수정**을 가능하게 하기 위해서입니다.

예를 들어, 이메일만 바꾸고 싶을 때:
```java
user.update(null, "new@email.com", null);
// userName과 password는 그대로, email만 변경
```

null이 아닌 값만 업데이트하므로, 원하는 필드만 선택적으로 수정할 수 있습니다.

### 3.3 Channel - 채널

```java
public class Channel extends BaseEntity {
    private final ChannelType type;  // PUBLIC 또는 PRIVATE (변경 불가)
    private String name;             // 채널 이름
    private String description;      // 채널 설명

    public Channel(ChannelType type, String name, String description) {
        super();
        this.type = type;
        this.name = name;
        this.description = description;
    }

    // ... getter, update 메서드
}
```

**왜 type은 final인가요?**

```java
private final ChannelType type;
```

`final`은 "한 번 설정하면 변경 불가"라는 뜻입니다.

채널의 종류(공개/비공개)는 채널의 **정체성**입니다. 공개 채널을 갑자기 비공개로 바꾸면:
- 이미 참여한 사람들의 권한 문제
- 메시지 접근 권한 문제
- 등등 복잡한 문제가 생김

그래서 처음 만들 때 정하고, 나중에 못 바꾸게 막았습니다.

### 3.4 ChannelType - 채널 종류

```java
public enum ChannelType {
    PUBLIC,   // 공개 채널: 누구나 참여 가능
    PRIVATE   // 비공개 채널: 초대받은 사람만 참여 가능
}
```

**왜 String 대신 enum을 쓰나요?**

만약 String을 쓰면:
```java
channel.setType("public");   // OK
channel.setType("Public");   // 대소문자 문제
channel.setType("publc");    // 오타
channel.setType("개인");     // 한글?
```

enum을 쓰면:
```java
channel.setType(ChannelType.PUBLIC);   // OK
channel.setType(ChannelType.PUBLC);    // 컴파일 에러! 바로 잡힘
```

enum은 **미리 정해진 값만 사용**하게 강제하므로, 오타나 잘못된 값을 컴파일 시점에 막을 수 있습니다.

### 3.5 Message - 메시지

```java
public class Message extends BaseEntity {
    private String content;           // 메시지 내용
    private final UUID channelId;     // 어떤 채널의 메시지인지
    private final UUID authorId;      // 누가 작성했는지

    public Message(String content, UUID channelId, UUID authorId) {
        super();
        this.content = content;
        this.channelId = channelId;
        this.authorId = authorId;
    }

    // ... getter, update 메서드
}
```

**왜 channelId와 authorId는 final인가요?**

메시지가 작성된 후에:
- 채널을 바꾸면? → 대화 흐름이 깨짐
- 작성자를 바꾸면? → 누가 뭘 말했는지 거짓이 됨

메시지의 **소속과 출처**는 변경되면 안 되는 정보입니다.

**왜 Channel, User 객체 대신 UUID만 저장하나요?**

```java
// 이렇게 안 하고
private final User author;
private final Channel channel;

// 이렇게 한 이유
private final UUID authorId;
private final UUID channelId;
```

이유 1: **순환 참조 방지**
- Message가 User를 가지고, User가 Message 목록을 가지면 복잡해짐

이유 2: **느슨한 결합**
- ID만 알면 필요할 때 User나 Channel을 조회할 수 있음
- Message 저장할 때 User 전체 정보가 필요 없음

이유 3: **직렬화 문제**
- 파일로 저장할 때 연결된 객체까지 다 저장하면 복잡해짐

---

## 4. Repository (레포지토리) - 데이터 저장소

Repository는 **"데이터를 어디에 어떻게 저장하고 가져올지"**를 담당합니다.

### 4.1 Repository 인터페이스

```java
public interface UserRepository {
    User save(User user);                    // 저장 (새로 만들기 + 수정)
    Optional<User> findById(UUID id);        // ID로 한 명 찾기
    List<User> findAll();                    // 전체 목록 가져오기
    void deleteById(UUID id);                // 삭제
    boolean existsById(UUID id);             // 존재 여부 확인
}
```

**왜 인터페이스를 먼저 만드나요?**

인터페이스는 **"계약서"**와 같습니다.

"나는 이런 기능들을 제공할 거야. 어떻게 구현하는지는 나중에 결정해."

장점:
1. **구현 변경이 쉬움**: 메모리 저장 → 파일 저장 → DB 저장으로 바꿔도 인터페이스는 그대로
2. **테스트가 쉬움**: 가짜(Mock) 저장소를 만들어서 테스트 가능
3. **역할 분담**: 인터페이스만 정해두면 여러 사람이 동시에 개발 가능

**Optional이 뭔가요?**

```java
Optional<User> findById(UUID id);
```

Optional은 "값이 있을 수도 있고 없을 수도 있다"는 뜻입니다.

```java
// 나쁜 방법: null 반환
User user = userRepository.findById(id);
if (user != null) {  // null 체크 깜빡하면 에러!
    // ...
}

// 좋은 방법: Optional 사용
Optional<User> userOpt = userRepository.findById(id);
userOpt.ifPresent(user -> {
    // user가 있을 때만 실행
});

// 또는
User user = userOpt.orElseThrow(() ->
    new NoSuchElementException("사용자를 찾을 수 없습니다")
);
```

Optional을 쓰면 "값이 없을 수 있다"는 것을 코드에서 명확히 알 수 있습니다.

### 4.2 JCF Repository (메모리 저장)

JCF = Java Collections Framework (HashMap, ArrayList 등)

```java
public class JCFUserRepository implements UserRepository {
    // 메모리에 데이터 저장 (프로그램 종료하면 사라짐)
    private final Map<UUID, User> data = new HashMap<>();

    @Override
    public User save(User user) {
        data.put(user.getId(), user);  // ID를 키로, User를 값으로 저장
        return user;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(data.get(id));  // 없으면 빈 Optional
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(data.values());  // 모든 User를 리스트로
    }

    @Override
    public void deleteById(UUID id) {
        data.remove(id);  // 삭제
    }

    @Override
    public boolean existsById(UUID id) {
        return data.containsKey(id);  // 존재 여부
    }
}
```

**왜 HashMap을 쓰나요?**

| 자료구조 | ID로 찾기 | 전체 조회 | 삭제 |
|---------|----------|----------|------|
| ArrayList | O(n) 느림 | O(1) 빠름 | O(n) 느림 |
| HashMap | O(1) 빠름 | O(n) | O(1) 빠름 |

우리가 자주 하는 작업은 "ID로 찾기"입니다. HashMap은 이 작업이 매우 빠릅니다.

**new ArrayList<>(data.values())는 왜 하나요?**

```java
public List<User> findAll() {
    return new ArrayList<>(data.values());  // 복사본 반환
    // return data.values(); 는 위험!
}
```

`data.values()`를 직접 반환하면:
- 외부에서 이 리스트를 수정하면 내부 데이터가 바뀜
- 의도치 않은 버그 발생 가능

복사본을 만들어 반환하면 외부에서 수정해도 원본은 안전합니다.

### 4.3 File Repository (파일 저장)

```java
public class FileUserRepository implements UserRepository {
    private final Path DIRECTORY;           // 저장 폴더 경로
    private final String EXTENSION = ".ser"; // 파일 확장자

    public FileUserRepository() {
        // 경로 설정: .../file-data-map/User/
        this.DIRECTORY = Paths.get(
            System.getProperty("user.dir"),
            "discodeit", "src", "main", "java",
            "com", "sprint", "mission", "discodeit",
            "file-data-map",
            User.class.getSimpleName()  // "User"
        );

        // 폴더가 없으면 생성
        if (Files.notExists(DIRECTORY)) {
            Files.createDirectories(DIRECTORY);
        }
    }
```

**파일 저장 방식 (save 메서드)**

```java
@Override
public User save(User user) {
    Path path = resolvePath(user.getId());  // 예: .../User/abc-123.ser

    try (
        FileOutputStream fos = new FileOutputStream(path.toFile());
        ObjectOutputStream oos = new ObjectOutputStream(fos)
    ) {
        oos.writeObject(user);  // 객체를 파일로 직렬화
    } catch (IOException e) {
        throw new RuntimeException(e);
    }
    return user;
}
```

**try-with-resources가 뭔가요?**

```java
try (
    FileOutputStream fos = new FileOutputStream(...);
    ObjectOutputStream oos = new ObjectOutputStream(fos)
) {
    // 파일 작업
}  // 자동으로 fos.close(), oos.close() 호출됨
```

파일을 열면 반드시 닫아야 합니다. try-with-resources 문법을 쓰면 자동으로 닫아줍니다.

**파일 읽기 방식 (findById 메서드)**

```java
@Override
public Optional<User> findById(UUID id) {
    Path path = resolvePath(id);

    if (Files.notExists(path)) {
        return Optional.empty();  // 파일이 없으면 빈 Optional
    }

    try (
        FileInputStream fis = new FileInputStream(path.toFile());
        ObjectInputStream ois = new ObjectInputStream(fis)
    ) {
        return Optional.of((User) ois.readObject());  // 파일 → 객체로 역직렬화
    } catch (IOException | ClassNotFoundException e) {
        throw new RuntimeException(e);
    }
}
```

### 4.4 JCF vs File Repository 비교

| 특성 | JCF (메모리) | File (파일) |
|------|-------------|-------------|
| 속도 | 매우 빠름 | 느림 (I/O 발생) |
| 데이터 유지 | 프로그램 종료 시 삭제 | 영구 보존 |
| 용량 제한 | 메모리 한계 | 디스크 용량까지 |
| 사용 시기 | 테스트, 프로토타입 | 실제 데이터 저장 |

---

## 5. Service (서비스) - 비즈니스 로직

Service는 **"실제 업무 규칙을 적용하는 곳"**입니다.

### 5.1 Service 인터페이스

```java
public interface UserService {
    User create(String userName, String email, String password);  // 회원가입
    User find(UUID id);                                           // 조회
    List<User> findAll();                                         // 전체 조회
    User update(UUID id, String userName, String email, String password);  // 수정
    void delete(UUID id);                                         // 탈퇴
}
```

### 5.2 Basic Service (권장 방식)

```java
public class BasicUserService implements UserService {
    // Repository를 주입받음 (의존성 주입)
    private final UserRepository userRepository;

    // 생성자에서 Repository를 받음
    public BasicUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User create(String username, String email, String password) {
        User newUser = new User(username, email, password);
        return userRepository.save(newUser);  // Repository에게 저장 위임
    }

    @Override
    public User find(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("User not found: " + id));
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public User update(UUID id, String userName, String email, String password) {
        User user = find(id);           // 먼저 찾고
        user.update(userName, email, password);  // 수정하고
        return userRepository.save(user);        // 다시 저장
    }

    @Override
    public void delete(UUID id) {
        userRepository.deleteById(id);
    }
}
```

**왜 Repository를 직접 만들지 않고 주입받나요?**

```java
// 나쁜 방법: 직접 생성
public class BasicUserService {
    private final UserRepository userRepository = new JCFUserRepository();
    // 이러면 JCF에서 File로 바꾸려면 코드 수정 필요!
}

// 좋은 방법: 주입받음
public class BasicUserService {
    private final UserRepository userRepository;

    public BasicUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        // 어떤 Repository든 받을 수 있음
    }
}
```

장점:
1. **유연성**: 나중에 저장 방식 바꾸기 쉬움
2. **테스트**: 가짜 Repository로 테스트 가능
3. **재사용성**: 같은 Service를 다른 환경에서 사용 가능

### 5.3 JCF Service (비교용 - 권장하지 않음)

```java
public class JCFUserService implements UserService {
    // Repository 없이 직접 HashMap 관리
    private final Map<UUID, User> userMap = new HashMap<>();

    @Override
    public User create(String userName, String email, String password) {
        User user = new User(userName, email, password);
        userMap.put(user.getId(), user);  // 직접 저장
        return user;
    }

    // ... 다른 메서드들도 직접 userMap 조작
}
```

**왜 이 방식은 권장하지 않나요?**

문제점:
1. **관심사 혼합**: Service가 비즈니스 로직 + 저장 로직 둘 다 담당
2. **변경 어려움**: 저장 방식 바꾸려면 Service 코드 전체 수정 필요
3. **테스트 어려움**: 저장 로직 분리가 안 되어 단위 테스트 힘듦

### 5.4 JCF Message Service (의존성 주입 예시)

```java
public class JCFMessageService implements MessageService {
    // 다른 Service들을 주입받음
    private final UserService userService;
    private final ChannelService channelService;

    private final List<Message> messageRepository = new ArrayList<>();

    public JCFMessageService(UserService userService, ChannelService channelService) {
        this.userService = userService;
        this.channelService = channelService;
    }

    @Override
    public Message create(String content, UUID channelId, UUID userId) {
        // 검증: 존재하는 사용자인지 확인
        if (userService.find(userId) == null) {
            throw new IllegalArgumentException("존재하지 않는 회원입니다.");
        }

        // 검증: 존재하는 채널인지 확인
        if (channelService.find(channelId) == null) {
            throw new IllegalArgumentException("존재하지 않는 채널입니다.");
        }

        // 검증 통과 후 메시지 생성
        Message message = new Message(content, channelId, userId);
        messageRepository.add(message);
        return message;
    }
}
```

**왜 메시지 생성 전에 User와 Channel을 검증하나요?**

데이터 무결성을 위해서입니다.

만약 검증 없이 메시지를 만들면:
- 존재하지 않는 사용자가 작성한 메시지?
- 삭제된 채널의 메시지?

이런 이상한 데이터가 생길 수 있습니다. 그래서 **메시지 생성 전에 반드시 관련 데이터가 존재하는지 확인**합니다.

---

## 6. 왜 이렇게 코딩했나? - 설계 원칙

### 6.1 계층 분리 (Layered Architecture)

```
┌─────────────────────────────────────────┐
│  JavaApplication (main)                 │  ← 프로그램 시작
├─────────────────────────────────────────┤
│  Service Layer                          │  ← 비즈니스 로직
│  (BasicUserService, BasicChannelService)│
├─────────────────────────────────────────┤
│  Repository Layer                       │  ← 데이터 접근
│  (JCFUserRepository, FileUserRepository)│
├─────────────────────────────────────────┤
│  Entity Layer                           │  ← 데이터 모델
│  (User, Channel, Message)               │
└─────────────────────────────────────────┘
```

**왜 계층을 나누나요?**

예시: 식당

| 계층 | 역할 | 식당 비유 |
|------|------|----------|
| main | 프로그램 실행 | 손님 |
| Service | 비즈니스 로직 | 주방장 |
| Repository | 데이터 저장/조회 | 식자재 창고 |
| Entity | 데이터 모델 | 레시피/메뉴판 |

손님(main)은 주방장(Service)에게 주문하고, 주방장은 창고(Repository)에서 재료를 가져와 요리합니다. 손님이 직접 창고에 가지 않습니다.

### 6.2 의존성 역전 원칙 (DIP)

**고수준 모듈이 저수준 모듈에 의존하면 안 된다.**

```
나쁜 구조:
BasicUserService → JCFUserRepository (구체적인 것에 의존)

좋은 구조:
BasicUserService → UserRepository (인터페이스에 의존)
                        ↑
              JCFUserRepository (구현체)
              FileUserRepository (구현체)
```

BasicUserService는 "UserRepository 인터페이스"에만 의존합니다. 실제로 JCF를 쓰든 File을 쓰든 상관없습니다.

### 6.3 단일 책임 원칙 (SRP)

**하나의 클래스는 하나의 책임만 가져야 한다.**

| 클래스 | 책임 |
|--------|------|
| User | 사용자 데이터 표현 |
| UserRepository | 사용자 데이터 저장/조회 |
| UserService | 사용자 관련 비즈니스 로직 |

만약 User 클래스가 자기 자신을 DB에 저장하는 기능까지 가지면?
- User 클래스가 너무 커짐
- DB가 바뀌면 User 클래스 수정 필요
- 테스트하기 어려움

책임을 나누면 각 클래스가 작고, 수정할 때 영향 범위가 적습니다.

### 6.4 인터페이스 분리 원칙 (ISP)

```java
// 각 엔티티마다 별도의 Repository 인터페이스
public interface UserRepository { ... }
public interface ChannelRepository { ... }
public interface MessageRepository { ... }
```

하나의 거대한 Repository 인터페이스 대신, 역할별로 분리했습니다.

### 6.5 Repository 패턴

**데이터 접근 로직을 추상화**합니다.

장점:
1. **저장소 교체 용이**: JCF → File → MySQL → MongoDB
2. **비즈니스 로직 집중**: Service는 저장 방식을 몰라도 됨
3. **테스트 용이**: Mock Repository로 단위 테스트 가능

---

## 7. 코드 실행 흐름 따라가기

### 7.1 사용자 생성 흐름 (Basic + JCF)

```
1. JavaApplication.main() 시작
   ↓
2. Repository 생성
   UserRepository userRepo = new JCFUserRepository();
   ↓
3. Service 생성 (Repository 주입)
   UserService userService = new BasicUserService(userRepo);
   ↓
4. 사용자 생성 요청
   userService.create("woody", "woody@codeit.com", "woody1234");
   ↓
5. BasicUserService.create() 실행
   - new User("woody", "woody@codeit.com", "woody1234")
   - BaseEntity 생성자에서 UUID 자동 생성
   ↓
6. userRepository.save(user) 호출
   ↓
7. JCFUserRepository.save() 실행
   - HashMap에 저장: data.put(user.getId(), user)
   ↓
8. 생성된 User 반환
```

### 7.2 메시지 생성 흐름 (검증 포함)

```
1. messageService.create("안녕하세요!", channelId, userId);
   ↓
2. JCFMessageService.create() 실행
   ↓
3. 사용자 검증
   if (userService.find(userId) == null) → 예외 발생
   ↓
4. 채널 검증
   if (channelService.find(channelId) == null) → 예외 발생
   ↓
5. 검증 통과 → Message 객체 생성
   new Message("안녕하세요!", channelId, userId)
   ↓
6. 저장소에 추가
   messageRepository.add(message)
   ↓
7. 생성된 Message 반환
```

### 7.3 JavaApplication의 테스트 흐름

```java
public static void main(String[] args) {
    // 1. Basic + JCF Repository 테스트
    testBasicWithJCFRepository();

    // 2. Basic + File Repository 테스트
    testBasicWithFileRepository();

    // 3. JCF Service 테스트 (비교용)
    testJCFServices();

    // 4. File Service 테스트 (비교용)
    testFileServices();

    // 5. 비교 분석 출력
    printComparison();
}
```

각 테스트에서는:
1. User CRUD (생성, 조회, 수정, 삭제) 테스트
2. Channel CRUD 테스트
3. Message CRUD 테스트
4. 결과를 파일로 저장 (`test-results/test-result-날짜.txt`)

---

## 8. 핵심 개념 정리

### 8.1 용어 정리

| 용어 | 의미 | 이 프로젝트에서 |
|------|------|----------------|
| Entity | 데이터 모델, DB 테이블에 대응 | User, Channel, Message |
| Repository | 데이터 저장소 접근 계층 | JCFUserRepository, FileUserRepository |
| Service | 비즈니스 로직 계층 | BasicUserService, JCFUserService |
| DI (의존성 주입) | 필요한 객체를 외부에서 주입받는 것 | 생성자에서 Repository 받기 |
| Interface | 계약서, 구현해야 할 메서드 정의 | UserRepository, UserService |

### 8.2 이 프로젝트의 4가지 구현 방식 비교

| 구현 방식 | 저장소 | Repository 패턴 | 권장 여부 |
|-----------|--------|-----------------|-----------|
| Basic + JCF | 메모리 | O | O (테스트용) |
| Basic + File | 파일 | O | O (실제 사용) |
| JCF Service | 메모리 | X | X (학습용) |
| File Service | 파일 | X | X (학습용) |

**권장 구조: Basic Service + Repository**

```java
// 권장하는 방식
UserRepository userRepo = new FileUserRepository();  // 원하는 저장소 선택
UserService userService = new BasicUserService(userRepo);  // 주입

// 저장 방식을 바꾸고 싶으면?
UserRepository userRepo = new JCFUserRepository();  // 이 줄만 바꾸면 됨!
UserService userService = new BasicUserService(userRepo);  // 이건 그대로
```

### 8.3 배운 점 요약

1. **계층을 나누면** 각 부분이 독립적이 되어 수정이 쉬워진다
2. **인터페이스를 쓰면** 구현체를 쉽게 바꿀 수 있다
3. **의존성을 주입받으면** 테스트와 유지보수가 쉬워진다
4. **Optional을 쓰면** null 처리를 명확하게 할 수 있다
5. **enum을 쓰면** 정해진 값만 사용하도록 강제할 수 있다

---

## 부록: 자주 하는 실수와 해결법

### A.1 null 체크 깜빡하기

```java
// 나쁜 예
User user = userService.find(id);
System.out.println(user.getUserName());  // user가 null이면 NullPointerException!

// 좋은 예
User user = userService.find(id);
if (user != null) {
    System.out.println(user.getUserName());
}

// 더 좋은 예 (Optional 사용)
userRepository.findById(id).ifPresent(user -> {
    System.out.println(user.getUserName());
});
```

### A.2 파일 스트림 닫지 않기

```java
// 나쁜 예
FileOutputStream fos = new FileOutputStream("file.txt");
fos.write(data);
// close() 안 하면 파일이 제대로 저장 안 될 수 있음!

// 좋은 예 (try-with-resources)
try (FileOutputStream fos = new FileOutputStream("file.txt")) {
    fos.write(data);
}  // 자동으로 close() 호출
```

### A.3 Collection 직접 반환하기

```java
// 나쁜 예
public List<User> getUsers() {
    return this.users;  // 외부에서 수정하면 내부 데이터 오염!
}

// 좋은 예
public List<User> getUsers() {
    return new ArrayList<>(this.users);  // 복사본 반환
}
```

---

> 이 문서가 Discodeit 프로젝트를 이해하는 데 도움이 되었기를 바랍니다.
> 질문이 있다면 코드를 직접 실행하면서 확인해보세요!
