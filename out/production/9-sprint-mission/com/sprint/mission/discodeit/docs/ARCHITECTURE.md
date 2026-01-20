# Discodeit 아키텍처 분석 문서

## 1. 개요

이 문서는 Discodeit 프로젝트의 아키텍처와 세 가지 서비스 구현 방식(JCF, File, Basic+Repository)을 비교 분석합니다.

## 2. 전체 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                     JavaApplication                         │
│                    (테스트 및 실행)                            │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Service Layer                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ UserService  │  │ChannelService│  │MessageService│       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
└─────────────────────────────────────────────────────────────┘
                              │
          ┌───────────────────┼───────────────────┐
          ▼                   ▼                   ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│   JCF Service   │ │  File Service   │ │  Basic Service  │
│  (직접 HashMap)  │ │ (직접 File I/O)  │ │ (Repository 위임)│
└─────────────────┘ └─────────────────┘ └────────┬────────┘
                                                  │
                                                  ▼
                                    ┌─────────────────────────┐
                                    │   Repository Layer      │
                                    │  ┌──────────────────┐   │
                                    │  │ JCF Repository   │   │
                                    │  │ File Repository  │   │
                                    │  └──────────────────┘   │
                                    └─────────────────────────┘
                                                  │
                                                  ▼
                                    ┌─────────────────────────┐
                                    │     Data Storage        │
                                    │  ┌─────────┐ ┌───────┐  │
                                    │  │ HashMap │ │ File  │  │
                                    │  └─────────┘ └───────┘  │
                                    └─────────────────────────┘
```

## 3. 서비스 구현 방식 비교

### 3.1 JCF Service (Java Collection Framework)

**구현 파일:**
- `JCFUserService.java`
- `JCFChannelService.java`
- `JCFMessageService.java`

**특징:**
- `HashMap<UUID, Entity>`를 사용한 메모리 기반 저장
- Service 클래스 내에서 직접 데이터 저장/조회 처리
- 의존성 없이 독립적으로 동작

**코드 예시:**
```java
public class JCFUserService implements UserService {
    private final Map<UUID, User> data = new HashMap<>();

    @Override
    public User create(String username, String email, String password) {
        User user = new User(username, email, password);
        data.put(user.getId(), user);
        return user;
    }

    @Override
    public User find(UUID id) {
        return Optional.ofNullable(data.get(id))
                .orElseThrow(() -> new NoSuchElementException("User not found"));
    }
}
```

**장점:**
- 구현이 단순하고 직관적
- 빠른 속도 (메모리 접근)
- 외부 의존성 없음

**단점:**
- 애플리케이션 종료 시 데이터 소멸
- Service와 데이터 접근 로직이 혼재
- 테스트 시 데이터 격리 어려움

---

### 3.2 File Service

**구현 파일:**
- `FileUserService.java`
- `FileChannelService.java`
- `FileMessageService.java`

**특징:**
- Java Serialization을 이용한 파일 기반 영속성
- 각 엔티티를 개별 `.ser` 파일로 저장
- Service 클래스 내에서 직접 File I/O 처리

**저장 경로:**
```
src/main/java/com/sprint/mission/discodeit/file-data-map/
├── User/
│   └── {uuid}.ser
├── Channel/
│   └── {uuid}.ser
└── Message/
    └── {uuid}.ser
```

**코드 예시:**
```java
public class FileUserService implements UserService {
    private final Path DIRECTORY;
    private final String EXTENSION = ".ser";

    @Override
    public User create(String username, String email, String password) {
        User user = new User(username, email, password);
        Path path = resolvePath(user.getId());
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(path.toFile()))) {
            oos.writeObject(user);
        }
        return user;
    }
}
```

**장점:**
- 데이터 영속성 보장
- 애플리케이션 재시작 후에도 데이터 유지
- 별도 데이터베이스 불필요

**단점:**
- 파일 I/O로 인한 성능 저하
- Service에 인프라 로직 혼재
- 동시성 이슈 발생 가능

---

### 3.3 Basic Service + Repository 패턴

**구현 파일:**

Service:
- `BasicUserService.java`
- `BasicChannelService.java`
- `BasicMessageService.java`

Repository Interface:
- `UserRepository.java`
- `ChannelRepository.java`
- `MessageRepository.java`

Repository Implementation:
- `JCFUserRepository.java`, `FileUserRepository.java`
- `JCFChannelRepository.java`, `FileChannelRepository.java`
- `JCFMessageRepository.java`, `FileMessageRepository.java`

**특징:**
- Repository 인터페이스를 통한 데이터 접근 추상화
- Service는 비즈니스 로직에만 집중
- 의존성 주입(DI)을 통한 저장소 선택

**코드 예시:**
```java
public class BasicUserService implements UserService {
    private final UserRepository userRepository;

    // 의존성 주입
    public BasicUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User create(String username, String email, String password) {
        User user = new User(username, email, password);
        return userRepository.save(user);  // Repository에 위임
    }

    @Override
    public User find(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
    }
}
```

**장점:**
- 관심사 분리 (SRP 준수)
- 저장소 교체 용이 (OCP 준수)
- 테스트 용이 (Mock Repository 주입 가능)
- 유연한 구성 가능

**단점:**
- 구조가 상대적으로 복잡
- 파일 수 증가
- 학습 곡선 존재

---

## 4. 비교 분석표

| 항목 | JCF Service | File Service | Basic + Repository |
|------|-------------|--------------|-------------------|
| **데이터 영속성** | X (메모리) | O (파일) | 선택 가능 |
| **구현 복잡도** | 낮음 | 중간 | 높음 |
| **성능** | 빠름 | 느림 | 저장소 의존 |
| **관심사 분리** | X | X | O |
| **테스트 용이성** | 낮음 | 낮음 | 높음 |
| **유연성** | 낮음 | 낮음 | 높음 |
| **의존성 주입** | X | X | O |
| **SOLID 원칙** | 부분 준수 | 부분 준수 | 준수 |

## 5. 설계 원칙 분석

### 5.1 단일 책임 원칙 (SRP - Single Responsibility Principle)

**JCF/File Service:**
- Service가 비즈니스 로직과 데이터 저장을 모두 담당
- SRP 위반

**Basic + Repository:**
- Service: 비즈니스 로직만 담당
- Repository: 데이터 접근만 담당
- SRP 준수

### 5.2 개방-폐쇄 원칙 (OCP - Open/Closed Principle)

**JCF/File Service:**
- 저장 방식 변경 시 Service 코드 수정 필요
- OCP 위반

**Basic + Repository:**
- 새로운 Repository 구현체 추가로 저장 방식 확장
- 기존 Service 코드 수정 불필요
- OCP 준수

### 5.3 의존성 역전 원칙 (DIP - Dependency Inversion Principle)

**JCF/File Service:**
- 고수준 모듈(Service)이 저수준 구현에 직접 의존
- DIP 위반

**Basic + Repository:**
- Service가 Repository 인터페이스에 의존
- 구체적인 구현은 런타임에 주입
- DIP 준수

```
[Before - JCF/File Service]
Service ──────────▶ HashMap/File (구체적 구현)

[After - Basic + Repository]
Service ──────────▶ Repository (추상화)
                         △
                         │
              ┌──────────┴──────────┐
              │                     │
       JCFRepository         FileRepository
```

## 6. 테스트 구성

### 6.1 테스트 시나리오

```java
// 1. 사용자 설정
static User setupUser(UserService userService) {
    User user = userService.create("woody", "woody@codeit.com", "woody1234");
    return user;
}

// 2. 채널 설정
static Channel setupChannel(ChannelService channelService) {
    Channel channel = channelService.create(
        ChannelType.PUBLIC, "공지", "공지 채널입니다."
    );
    return channel;
}

// 3. 메시지 생성 테스트
static void messageCreateTest(MessageService messageService,
                               Channel channel, User author) {
    Message message = messageService.create(
        "안녕하세요.", channel.getId(), author.getId()
    );
    log("메시지 생성: " + message.getId());
}
```

### 6.2 무결성 테스트

```java
static void integrityTest(MessageService messageService,
                          Channel channel, User user) {
    // 존재하지 않는 사용자로 메시지 생성 시도
    try {
        messageService.create("테스트", channel.getId(), UUID.randomUUID());
    } catch (IllegalArgumentException | NoSuchElementException e) {
        log("예상된 예외 발생: " + e.getMessage());
    }

    // 존재하지 않는 채널로 메시지 생성 시도
    try {
        messageService.create("테스트", UUID.randomUUID(), user.getId());
    } catch (IllegalArgumentException | NoSuchElementException e) {
        log("예상된 예외 발생: " + e.getMessage());
    }
}
```

## 7. 권장 사항

### 7.1 개발/테스트 환경
- **Basic Service + JCF Repository** 권장
- 빠른 테스트 실행
- 데이터 격리 용이

### 7.2 운영 환경
- **Basic Service + File Repository** 권장
- 데이터 영속성 보장
- 향후 DB Repository로 교체 용이

### 7.3 학습/프로토타입
- **JCF Service** 권장
- 단순한 구조로 빠른 이해
- 최소한의 코드로 동작 확인

## 8. 향후 개선 방향

1. **데이터베이스 Repository 구현**
   - JPA/JDBC 기반 Repository 추가
   - 트랜잭션 관리 도입

2. **예외 처리 표준화**
   - 커스텀 예외 클래스 정의
   - 일관된 예외 처리 전략

3. **Spring Framework 도입**
   - IoC/DI 컨테이너 활용
   - AOP를 통한 횡단 관심사 처리

4. **테스트 자동화**
   - JUnit 단위 테스트 추가
   - Mock Repository를 활용한 Service 테스트

## 9. 결론

Repository 패턴을 적용한 Basic Service 구현이 가장 유연하고 확장 가능한 아키텍처를 제공합니다. 초기에는 복잡해 보일 수 있지만, 프로젝트 규모가 커질수록 유지보수성과 테스트 용이성에서 큰 이점을 얻을 수 있습니다.

단, 프로젝트의 규모와 요구사항에 따라 적절한 구현 방식을 선택하는 것이 중요합니다. 간단한 프로토타입이나 학습 목적이라면 JCF Service로 충분하며, 영속성이 필요하면 File Service를, 확장성과 유연성이 중요하다면 Repository 패턴을 적용하는 것을 권장합니다.
