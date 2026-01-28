# Discodeit 프로젝트 구조


---

## 1. 프로젝트가 뭘 하는 건가요?

Discodeit은 **Discord(디스코드)** 같은 채팅 앱의 백엔드(서버)를 만드는 프로젝트입니다.

쉽게 말해서:
- **사용자(User)**: 채팅에 참여하는 사람
- **채널(Channel)**: 대화방 (예: "공지방", "잡담방")
- **메시지(Message)**: 대화 내용

이 세 가지를 **생성, 조회, 수정, 삭제(CRUD)** 하는 기능을 구현합니다.

---

## 2. 폴더 구조 한눈에 보기

```
discodeit/
│
└── src/main/java/com/sprint/mission/discodeit/
    │
    ├── entity/                ← "데이터가 뭔지" 정의
    │
    ├── repository/            ← "데이터를 어디에 저장할지" 담당
    │
    ├── service/               ← "무슨 기능을 할지" 담당
    │
    ├── JavaApplication.java   ← 프로그램 시작점
    │
    └── docs/                  ← 문서 모음
```

---

## 3. 각 폴더가 하는 일

### 3.1 entity 폴더 - "데이터가 뭔지 정의"

**비유**: 주민등록증 양식 같은 것

```
entity/
├── BaseEntity.java      ← 공통 정보 (ID, 생성시간, 수정시간)
├── User.java            ← 사용자 정보 (이름, 이메일, 비밀번호)
├── Channel.java         ← 채널 정보 (이름, 설명, 타입)
├── ChannelType.java     ← 채널 종류 (공개/비공개)
└── Message.java         ← 메시지 정보 (내용, 작성자, 채널)
```

**예시 - User.java가 담는 정보:**
```java
User {
    id: "abc-123"           // 고유번호
    username: "홍길동"        // 이름
    email: "hong@email.com" // 이메일
    password: "1234"        // 비밀번호
    createdAt: "2024-01-01" // 가입일
}
```

---

### 3.2 repository 폴더 - "데이터를 어디에 저장할지"

**비유**: 서류를 보관하는 캐비닛

```
repository/
├── UserRepository.java        ← 인터페이스 (설계도)
├── ChannelRepository.java
├── MessageRepository.java
│
├── jcf/                       ← 메모리에 저장 (컴퓨터 끄면 사라짐)
│   ├── JCFUserRepository.java
│   ├── JCFChannelRepository.java
│   └── JCFMessageRepository.java
│
└── file/                      ← 파일에 저장 (컴퓨터 꺼도 유지)
    ├── FileUserRepository.java
    ├── FileChannelRepository.java
    └── FileMessageRepository.java
```

**JCF vs File 차이점:**

| 구분 | JCF (메모리) | File (파일) |
|------|-------------|-------------|
| 저장 위치 | 컴퓨터 RAM | 하드디스크 |
| 속도 | 빠름 | 느림 |
| 데이터 유지 | 프로그램 끄면 삭제 | 영구 보관 |
| 용도 | 테스트용 | 실제 서비스용 |

---

### 3.3 service 폴더 - "무슨 기능을 할지"

**비유**: 은행 직원 (고객 요청을 처리하는 사람)

```
service/
├── UserService.java           ← 인터페이스 (설계도)
├── ChannelService.java
├── MessageService.java
│
├── jcf/                       ← 직접 메모리에 저장하는 방식
│   ├── JCFUserService.java
│   └── ...
│
├── file/                      ← 직접 파일에 저장하는 방식
│   ├── FileUserService.java
│   └── ...
│
└── basic/                     ← Repository에 맡기는 방식 (권장!)
    ├── BasicUserService.java
    ├── BasicChannelService.java
    └── BasicMessageService.java
```

**Service가 제공하는 기능:**
- `create()` - 새로 만들기
- `find()` - 하나 찾기
- `findAll()` - 전부 찾기
- `update()` - 수정하기
- `delete()` - 삭제하기

---

## 4. 세 가지 구현 방식 비교

이 프로젝트에는 같은 기능을 **3가지 다른 방식**으로 구현해 놨습니다.

### 방식 1: JCF Service (가장 단순)

```
[JCFUserService] ──저장──▶ [HashMap (메모리)]
```

- 직접 HashMap에 저장
- 코드가 간단함
- 프로그램 끄면 데이터 사라짐

### 방식 2: File Service (중간)

```
[FileUserService] ──저장──▶ [.ser 파일 (하드디스크)]
```

- 직접 파일에 저장
- 데이터가 영구 보관됨
- 코드가 조금 복잡함

### 방식 3: Basic Service + Repository (권장)

```
[BasicUserService] ──요청──▶ [Repository] ──저장──▶ [메모리 or 파일]
```

- Service는 "뭘 할지"만 결정
- Repository가 "어디에 저장할지" 담당
- **역할 분리**가 잘 됨
- 나중에 저장 방식 바꾸기 쉬움

---

## 5. 왜 이렇게 나눠 놓았나요?

### 쉬운 비유: 음식점

```
[손님] ──주문──▶ [웨이터] ──전달──▶ [주방] ──요리──▶ [냉장고]
                  (Service)      (Repository)    (저장소)
```

- **손님**: 프로그램을 사용하는 코드
- **웨이터(Service)**: 주문받고 전달하는 역할
- **주방(Repository)**: 실제로 음식 만드는 역할
- **냉장고(저장소)**: 재료 보관 장소

**왜 나누는 게 좋을까요?**

1. **웨이터가 바뀌어도** 주방은 그대로
2. **냉장고가 바뀌어도** 웨이터는 그대로
3. 각자 자기 일만 잘하면 됨!

---

## 6. 실제 코드 흐름

### 사용자 생성 예시

```java
// 1. Service에게 "사용자 만들어줘" 요청
User user = userService.create("홍길동", "hong@email.com", "1234");

// 2. Service 내부에서 일어나는 일:
//    - User 객체 생성
//    - Repository에게 저장 요청
//    - 저장된 User 반환

// 3. 결과
user.getId();       // "abc-123" (자동 생성된 ID)
user.getUsername(); // "홍길동"
```

### 메시지 생성 예시 (연관관계)

```java
// 메시지를 보내려면 사용자와 채널이 필요!
User user = userService.create("홍길동", "hong@email.com", "1234");
Channel channel = channelService.create(PUBLIC, "공지방", "공지 채널");

// 메시지 생성 (누가, 어디에, 무슨 내용)
Message msg = messageService.create("안녕하세요!", channel.getId(), user.getId());
```

---

## 7. 파일 저장 위치

프로그램 실행 시 자동으로 생성되는 폴더:

```
src/main/java/com/sprint/mission/discodeit/
│
├── file-data-map/           ← 데이터 저장 폴더
│   ├── User/
│   │   └── abc-123.ser      ← 사용자 데이터
│   ├── Channel/
│   │   └── def-456.ser      ← 채널 데이터
│   └── Message/
│       └── ghi-789.ser      ← 메시지 데이터
│
└── test-results/            ← 테스트 결과 폴더
    └── test_result_20240120_143025.txt
```

---

## 8. 인터페이스란?

코드에서 `UserService.java`, `UserRepository.java` 같은 파일은 **인터페이스**입니다.

**비유**: 리모컨 버튼

```
리모컨 버튼 (인터페이스)     실제 동작 (구현체)
─────────────────────────────────────────
[전원] 버튼           →    TV 켜기/끄기
[채널+] 버튼          →    다음 채널로 이동
[음량+] 버튼          →    소리 크게 하기
```

- **인터페이스**: "이런 버튼(기능)이 있어야 해!"라고 정의
- **구현체**: 버튼을 눌렀을 때 실제로 하는 일

```java
// 인터페이스 (설계도)
interface UserService {
    User create(String name, String email, String password);
    User find(UUID id);
}

// 구현체 (실제 동작)
class JCFUserService implements UserService {
    // create() 실제 코드...
    // find() 실제 코드...
}
```

---

## 9. 용어 정리

| 용어 | 쉬운 설명 |
|------|----------|
| Entity | 데이터의 모양을 정의한 것 (설계도) |
| Repository | 데이터를 저장하고 꺼내오는 곳 (창고) |
| Service | 실제 기능을 수행하는 곳 (직원) |
| Interface | 기능 목록만 적어놓은 것 (메뉴판) |
| Implementation | 인터페이스를 실제로 구현한 것 (요리) |
| CRUD | Create, Read, Update, Delete의 약자 |
| UUID | 고유한 ID를 만들어주는 것 |
| Serialization | 객체를 파일로 저장하는 기술 |

---

## 10. 정리

```
┌────────────────────────────────────────────────────────┐
│                    프로젝트 구조 요약                      │
├────────────────────────────────────────────────────────┤
│                                                        │
│   Entity (데이터 정의)                                    │
│      └── User, Channel, Message                        │
│                                                        │
│   Repository (저장소)                                    │
│      ├── JCF: 메모리에 저장 (임시)                          │
│      └── File: 파일에 저장 (영구)                          │
│                                                        │
│   Service (기능 처리)                                    │
│      ├── JCF Service: 직접 메모리 사용                     │
│      ├── File Service: 직접 파일 사용                     │
│      └── Basic Service: Repository에 위임 (권장!)         │
│                                                        │
└────────────────────────────────────────────────────────┘
```

이 구조를 이해하면 대부분의 백엔드 프로젝트를 이해할 수 있습니다!
