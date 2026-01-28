# Discodeit 프로젝트

Discord와 유사한 메시징 플랫폼의 백엔드 서비스 구현 프로젝트입니다.

## 프로젝트 소개

Discodeit은 사용자, 채널, 메시지를 관리하는 메시징 시스템입니다. 이 프로젝트는 다양한 서비스 구현 패턴(JCF, File, Basic+Repository)을 학습하고 비교하기 위한 교육용 프로젝트입니다.

### 주요 기능

- **사용자 관리**: 회원 생성, 조회, 수정, 삭제 (CRUD)
- **채널 관리**: 채널 생성, 조회, 수정, 삭제 (CRUD)
- **메시지 관리**: 메시지 생성, 조회, 수정, 삭제 (CRUD)
- **데이터 무결성 검증**: 메시지 생성 시 사용자/채널 존재 여부 확인

### 엔티티 구조

```
BaseEntity (추상 클래스)
├── User (사용자)
│   - username, email, password
├── Channel (채널)
│   - type (PUBLIC/PRIVATE), name, description
└── Message (메시지)
    - content, channelId, authorId
```

## 프로젝트 구조

```
discodeit/
├── src/main/java/com/sprint/mission/discodeit/
│   ├── entity/                                   # 엔티티 클래스
│   │   ├── BaseEntity.java
│   │   ├── User.java
│   │   ├── Channel.java
│   │   ├── ChannelType.java
│   │   └── Message.java
│   ├── repository/                               # Repository 인터페이스 및 구현체
│   │   ├── UserRepository.java
│   │   ├── ChannelRepository.java
│   │   ├── MessageRepository.java
│   │   ├── jcf/                                  # JCF(메모리) 기반 구현
│   │   │   ├── JCFUserRepository.java
│   │   │   ├── JCFChannelRepository.java
│   │   │   └── JCFMessageRepository.java
│   │   └── file/                                 # 파일 기반 구현
│   │       ├── FileUserRepository.java
│   │       ├── FileChannelRepository.java
│   │       └── FileMessageRepository.java
│   ├── service/                                  # Service 인터페이스 및 구현체
│   │   ├── UserService.java
│   │   ├── ChannelService.java
│   │   ├── MessageService.java
│   │   ├── jcf/                                  # JCF 직접 구현
│   │   ├── file/                                 # File 직접 구현
│   │   └── basic/                                # Repository 패턴 사용
│   │       ├── BasicUserService.java
│   │       ├── BasicChannelService.java
│   │       └── BasicMessageService.java
│   ├── file-data-map/                            # 파일 저장소 (자동 생성)
│   ├── test-results/                             # 테스트 결과 파일 (자동 생성)
│   └── JavaApplication.java                      # 메인 실행 클래스
└── docs/
    ├── README.md                                 # 프로젝트 소개
    ├── PROJECTARCHITECTURE.md                    # 프로젝트 분석
    └── ARCHITECTURE.md                           # 아키텍처 분석 문서
```

## 실행 방법

### 요구 사항

- Java 17 이상
- Gradle 9.0 이상

### 빌드

```bash
cd discodeit
./gradlew build
```

### 실행

```bash
./gradlew run
```

또는 IDE(IntelliJ IDEA)에서 `JavaApplication.java` 파일을 직접 실행할 수 있습니다.

### 실행 결과

프로그램 실행 시 다음 테스트가 순차적으로 수행됩니다:

1. **Basic + JCF Repository 테스트**: Repository 패턴 + 메모리 저장소
2. **Basic + File Repository 테스트**: Repository 패턴 + 파일 저장소
3. **JCF Service 테스트**: 메모리 기반 직접 구현
4. **File Service 테스트**: 파일 기반 직접 구현
5. **서비스 구현체 비교 분석**: 각 구현 방식의 특징 비교

테스트 결과는 콘솔에 출력되며, 동시에 `test-results/` 폴더에 타임스탬프가 포함된 파일로 저장됩니다.

```
test-results/
└── test_result_20260120_143025.txt
```

## 서비스 구현 방식

### 1. JCF Service (Java Collection Framework)
- `HashMap`을 사용한 메모리 기반 저장
- 애플리케이션 종료 시 데이터 소멸
- 빠른 속도, 테스트 용도에 적합

### 2. File Service
- Java Serialization을 이용한 파일 기반 저장
- 애플리케이션 종료 후에도 데이터 유지
- 영속성 보장, 실제 서비스 환경에 적합

### 3. Basic Service + Repository
- Repository 패턴을 적용한 구현
- Service와 데이터 접근 계층 분리
- JCF Repository 또는 File Repository 선택 가능
- 유연성과 테스트 용이성 제공

자세한 아키텍처 분석은 [ARCHITECTURE.md](./ARCHITECTURE.md)를 참조하세요.

## 테스트 시나리오

```java
// 1. 사용자 생성
User user = userService.create("woody", "woody@codeit.com", "woody1234");

// 2. 채널 생성
Channel channel = channelService.create(ChannelType.PUBLIC, "공지", "공지 채널입니다.");

// 3. 메시지 생성 (사용자와 채널 연결)
Message message = messageService.create("안녕하세요.", channel.getId(), user.getId());

// 4. 무결성 테스트 - 존재하지 않는 사용자/채널로 메시지 생성 시도
messageService.create("테스트", channel.getId(), UUID.randomUUID()); // 예외 발생
messageService.create("테스트", UUID.randomUUID(), user.getId());    // 예외 발생
```
