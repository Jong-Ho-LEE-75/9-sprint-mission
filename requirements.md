# 스프린트 미션 6 기본 요구사항 정리

## 1. 데이터베이스 환경 설정

- DB명: `discodeit`
- 유저: `discodeit_user`
- 패스워드: `discodeit1234`

## 2. ERD (u0ghedzoz-image.png)

### 테이블 구조

| 테이블 | 컬럼 | 타입/제약 |
|---|---|---|
| **binary_contents** | id | uuid PK |
| | created_at | timestamptz NN |
| | file_name | varchar(255) |
| | size | bigint |
| | content_type | varchar(100) |
| **users** | id | uuid PK |
| | created_at | timestamptz NN |
| | updated_at | timestamptz |
| | username | varchar(50) UK NN |
| | email | varchar(100) UK NN |
| | password | varchar(60) NN |
| | profile_id | uuid FK → binary_contents(id) ON DELETE SET NULL |
| **user_statuses** | id | uuid PK |
| | created_at | timestamptz NN |
| | updated_at | timestamptz |
| | user_id | uuid FK UK NN → users(id) ON DELETE CASCADE |
| | last_active_at | timestamptz NN |
| **channels** | id | uuid PK |
| | created_at | timestamptz NN |
| | updated_at | timestamptz |
| | name | varchar(100) |
| | description | varchar(500) |
| | type | varchar(10) NN ENUM(PUBLIC, PRIVATE) |
| **read_statuses** | id | uuid PK |
| | created_at | timestamptz NN |
| | updated_at | timestamptz |
| | user_id | uuid FK NN UK(user_id,channel_id) → users(id) ON DELETE CASCADE |
| | channel_id | uuid FK NN UK(user_id,channel_id) → channels(id) ON DELETE CASCADE |
| | last_read_at | timestamptz NN |
| **messages** | id | uuid PK |
| | created_at | timestamptz NN |
| | updated_at | timestamptz |
| | content | text |
| | channel_id | uuid FK NN → channels(id) ON DELETE CASCADE |
| | author_id | uuid FK → users(id) ON DELETE SET NULL |
| **message_attachments** | message_id | uuid FK NN → messages(id) ON DELETE CASCADE |
| | attachment_id | uuid FK NN → binary_contents(id) ON DELETE CASCADE |

DDL 파일 경로: `src/main/resources/schema.sql`

---

## 3. Spring Data JPA 적용 (xs6bzcvs6-image.png)

- 추가 의존성: `spring-boot-starter-data-jpa`, `postgresql`
- `application.yaml`에 DB 연결 설정
- SQL 로그 설정 (show-sql, format_sql)

---

## 4. 엔티티 추상 클래스 (xs6bzcvs6-image.png)

패키지: `com.sprint.mission.discodeit.entity.base`

```
«abstract» BaseEntity
  - UUID id
  - Instant createdAt  (@CreatedDate 자동 설정)

«abstract» BaseUpdatableEntity extends BaseEntity
  - Instant updatedAt  (@LastModifiedDate 자동 설정)
```

- BinaryContent → BaseEntity 상속 (updatedAt 없음)
- Channel, Message, User, ReadStatus, UserStatus → BaseUpdatableEntity 상속
- Serializable 인터페이스 제거

---

## 5. 클래스 참조 관계 수정 (pq5iz92wt-image.png)

| 엔티티 | 필드 변경 |
|---|---|
| User | `UUID profileId` → `BinaryContent profile` |
| | `UserStatus status` (양방향) 추가 |
| UserStatus | `UUID userId` → `User user` |
| ReadStatus | `UUID userId` → `User user`, `UUID channelId` → `Channel channel` |
| Message | `UUID channelId` → `Channel channel`, `UUID authorId` → `User author`, `List<UUID> attachmentIds` → `List<BinaryContent> attachments` |

---

## 6. 연관관계 매핑 정보

| 엔티티 관계 | 다중성 | 방향성 | 부모-자식 | 연관관계 주인 |
|---|---|---|---|---|
| User : UserStatus | 1:1 | 양방향 | 부모: User, 자식: UserStatus | UserStatus (user_id FK) |
| User : BinaryContent (profile) | N:1 | User→BinaryContent 단방향 | 부모: BinaryContent, 자식: User | User (profile_id FK) |
| Channel : ReadStatus | 1:N | 단방향 (ReadStatus→Channel) | 부모: Channel, 자식: ReadStatus | ReadStatus (channel_id FK) |
| User : ReadStatus | 1:N | 단방향 (ReadStatus→User) | 부모: User, 자식: ReadStatus | ReadStatus (user_id FK) |
| Channel : Message | 1:N | 단방향 (Message→Channel) | 부모: Channel, 자식: Message | Message (channel_id FK) |
| User : Message (author) | 1:N | 단방향 (Message→User) | 부모: User, 자식: Message | Message (author_id FK) |
| Message : BinaryContent (attachments) | N:N | 단방향 (Message→BinaryContent) | 부모: Message, 자식: BinaryContent | message_attachments 조인 테이블 |

---

## 7. JPA 어노테이션 적용

- `@Entity`, `@Table` 추가
- `@Column`, `@Enumerated` 추가
- `@OneToOne`, `@ManyToOne`, `@ManyToMany` 추가
- `@JoinColumn`, `@JoinTable` 추가

### cascade / orphanRemoval

- User.status: `cascade=ALL, orphanRemoval=true` (User 삭제 시 UserStatus 삭제)
- Channel 삭제 시: DB CASCADE로 Message, ReadStatus 삭제 (schema.sql)
- Message 삭제 시: 서비스에서 BinaryContent 수동 삭제 후 Message 삭제

---

## 8. Repository → JpaRepository 전환

- 기존 인터페이스를 `JpaRepository` 상속으로 변경
- `FileRepository`, `JCFRepository` 구현체 전체 삭제
- 필요한 쿼리 메소드 추가 (findAllByChannel_Id 등)

---

## 9. 서비스 레이어 수정

- `@Transactional` 적용 (쓰기: @Transactional, 읽기: @Transactional(readOnly=true))
- 영속성 전이/변경 감지/지연 로딩 활용
- 모든 반환 타입 → DTO로 변경

---

## 10. DTO 구조 (hd4c6g1of-image.png)

```
BinaryContentDto: id, fileName, size, contentType   ← bytes 없음 (API spec 기준)
UserDto: id, username, email, BinaryContentDto profile, Boolean online
ChannelDto: id, type, name, description, List<UserDto> participants, Instant lastMessageAt
MessageDto: id, createdAt, updatedAt, content, UUID channelId, UserDto author, List<BinaryContentDto> attachments
ReadStatusDto: id, UUID userId, UUID channelId, Instant lastReadAt
UserStatusDto: id, UUID userId, Instant lastActiveAt
```

---

## 11. Mapper (buo7cmjvp-image.png)

패키지: `com.sprint.mission.discodeit.mapper`

| Mapper | 의존성 | 메소드 |
|---|---|---|
| BinaryContentMapper | - | BinaryContentDto toDto(BinaryContent) |
| UserMapper | BinaryContentMapper | UserDto toDto(User) |
| ReadStatusMapper | - | ReadStatusDto toDto(ReadStatus) |
| UserStatusMapper | - | UserStatusDto toDto(UserStatus) |
| MessageMapper | BinaryContentMapper, UserMapper | MessageDto toDto(Message) |
| ChannelMapper | MessageRepository, ReadStatusRepository, UserMapper | ChannelDto toDto(Channel) |

---

## 12. BinaryContent 저장 고도화

- `BinaryContent` 엔티티: `bytes` 필드 제거 (메타정보만 보관)
- 인터페이스: `com.sprint.mission.discodeit.storage.BinaryContentStorage` (nqt5zw2pk-image.png)

```
interface BinaryContentStorage:
  + UUID put(UUID, byte[])
  + InputStream get(UUID)
  + ResponseEntity<?> download(BinaryContentDto)
```

- 구현체: `LocalBinaryContentStorage` (skptrmm5p-image.png)
  - `discodeit.storage.type = local` 일 때만 Bean 등록
  - 설정값: `discodeit.storage.local.root-path`
  - `@PostConstruct` init(): 루트 디렉토리 초기화
  - `resolvePath(UUID)`: `{root}/{UUID}` 경로 규칙

- 다운로드 엔드포인트 추가 (5qwe2kqno-image.png):
  - `GET /api/binaryContents/{binaryContentId}/download`

---

## 13. 페이징 (wj4q7nhn3, x7qjncxm0-image.png)

- 메시지 목록: 50개씩, createdAt 내림차순
- `PageResponse<T>` DTO: content, number, size, hasNext, totalElements(nullable)
- `PageResponseMapper`: `fromSlice(Slice<T>)`, `fromPage(Page<T>)` 제네릭 메소드
- 패키지: `com.sprint.mission.discodeit.dto.response`, `com.sprint.mission.discodeit.mapper`

---

## 검증 체크리스트

- [ ] schema.sql 작성 및 테이블 생성
- [ ] BaseEntity, BaseUpdatableEntity 추상 클래스
- [ ] createdAt/updatedAt @CreatedDate/@LastModifiedDate 자동 설정
- [ ] 엔티티 클래스 참조 관계 수정 (UUID→Entity)
- [ ] JPA 어노테이션 적용
- [ ] cascade/orphanRemoval 정의
- [ ] JpaRepository 전환 (File*, JCF* 삭제)
- [ ] 서비스 @Transactional 적용
- [ ] DTO 정의 (BinaryContentDto.bytes 없음)
- [ ] Mapper 컴포넌트 구현
- [ ] BinaryContent bytes 필드 제거
- [ ] BinaryContentStorage 인터페이스 + LocalBinaryContentStorage 구현
- [ ] 다운로드 API 추가
- [ ] PageResponse, PageResponseMapper 구현
- [ ] 메시지 조회 페이징 적용
- [ ] API spec(api-docs_1.1.json) 기준 모든 응답 타입 일치
