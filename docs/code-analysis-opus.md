# API 스펙 v1.2 기준 코드 분석 보고서 (Opus)

## 1. API 엔드포인트 준수 현황

### api-docs.json (v1.2) 기준

| 엔드포인트 | 스펙 | 구현 | 상태 |
|---|---|---|---|
| POST /api/auth/login | O | O | 정상 |
| GET /api/users | O | O | 정상 |
| POST /api/users | O | O | 정상 |
| PATCH /api/users/{userId} | O | O | 정상 |
| DELETE /api/users/{userId} | O | O | 정상 |
| PATCH /api/users/{userId}/userStatus | O | O | 정상 |
| GET /api/channels | O | O | 정상 |
| POST /api/channels/public | O | O | 정상 |
| POST /api/channels/private | O | O | 정상 |
| PATCH /api/channels/{channelId} | O | O | 정상 |
| DELETE /api/channels/{channelId} | O | O | 정상 |
| GET /api/messages | O | O | 정상 |
| POST /api/messages | O | O | 정상 |
| PATCH /api/messages/{messageId} | O | O | 정상 |
| DELETE /api/messages/{messageId} | O | O | 정상 |
| GET /api/readStatuses | O | O | 정상 |
| POST /api/readStatuses | O | O | 정상 |
| PATCH /api/readStatuses/{readStatusId} | O | O | 정상 |
| GET /api/binaryContents | O | O | 정상 |
| GET /api/binaryContents/{id} | O | O | 정상 |
| GET /api/binaryContents/{id}/download | O | O | 정상 |

**결론: 모든 API 엔드포인트가 스펙대로 구현됨**

---

## 2. DTO/스키마 준수 현황

| DTO | 스펙 필드 | 구현 필드 | 상태 |
|-----|---------|---------|------|
| UserDto | id, username, email, profile, online | 동일 | 정상 |
| UserCreateRequest | username, email, password | 동일 | 정상 |
| UserUpdateRequest | newUsername, newEmail, newPassword | 동일 | 정상 |
| UserStatusUpdateRequest | newLastActiveAt | 동일 | 정상 |
| UserStatusDto | id, userId, lastActiveAt | 동일 | 정상 |
| ChannelDto | id, type, name, description, participants, lastMessageAt | 동일 | 정상 |
| PublicChannelCreateRequest | name, description | 동일 | 정상 |
| PrivateChannelCreateRequest | participantIds | 동일 | 정상 |
| PublicChannelUpdateRequest | newName, newDescription | 동일 | 정상 |
| MessageDto | id, createdAt, updatedAt, content, channelId, author, attachments | 동일 | 정상 |
| MessageCreateRequest | content, channelId, authorId | 동일 | 정상 |
| MessageUpdateRequest | newContent | 동일 | 정상 |
| ReadStatusDto | id, userId, channelId, lastReadAt | 동일 | 정상 |
| ReadStatusCreateRequest | userId, channelId, lastReadAt | 동일 | 정상 |
| ReadStatusUpdateRequest | newLastReadAt | 동일 | 정상 |
| LoginRequest | username, password | 동일 | 정상 |
| BinaryContentDto | id, fileName, size, contentType | 동일 | 정상 |
| PageResponse | content, number, size, hasNext, totalElements | 동일 | 정상 |
| Pageable | page, size, sort | 동일 | 정상 |

**결론: 모든 DTO가 API 스펙과 일치**

---

## 3. 기본 요구사항 (스프린트 미션 6-1) 준수 현황

| 항목 | 상태 | 비고 |
|------|------|------|
| Spring Data JPA Repository 전환 | 정상 | 모든 Repository가 JpaRepository 상속 |
| Entity 설계 (BaseEntity, BaseUpdatableEntity) | 정상 | UUID PK, JPA Auditing |
| DTO/Mapper 패턴 | 정상 | MapStruct + 수동 ChannelMapper |
| Service 레이어 인터페이스/구현체 분리 | 정상 | |
| Controller 레이어 (REST API) | 정상 | |
| 파일 저장소 (BinaryContentStorage) | 정상 | LocalBinaryContentStorage |
| 예외 처리 (GlobalExceptionHandler) | 정상 | 400/404/500 매핑 |
| ddl-auto: validate | 정상 | schema.sql 수동 관리 |

---

## 4. 심화 요구사항 (스프린트 미션 6-2) 준수 현황

| 항목 | 상태 | 비고 |
|------|------|------|
| N+1 문제 해결 (JOIN FETCH) | 정상 | MessageRepository, ChannelRepository, UserRepository |
| N+1 문제 해결 (@BatchSize) | 정상 | Message.attachments에 @BatchSize(100) |
| N+1 문제 해결 (배치 조회) | 정상 | findLastCreatedAtByChannelIds() |
| OSIV 비활성화 | 정상 | open-in-view: false |
| @Transactional(readOnly = true) | 정상 | 모든 조회 메서드에 적용 |
| MapStruct 적용 | 정상 | UserMapper, MessageMapper, ReadStatusMapper, BinaryContentMapper, UserStatusMapper |
| 페이지 기반 페이지네이션 | 정상 | PageResponse의 number 필드 사용, Slice 기반 |

---

## 5. 불필요한 코드 목록

### 5-1. 컨트롤러에서 호출되지 않는 Service 메서드

| 파일 | 메서드 | 이유 |
|------|--------|------|
| `UserService.find(UUID)` | 인터페이스 선언 | API 스펙에 GET /api/users/{userId} 엔드포인트 없음 |
| `BasicUserService.find(UUID)` | 구현체 | 위와 동일 |
| `ReadStatusService.find(UUID)` | 인터페이스 선언 | 어디에서도 호출 안 됨 |
| `BasicReadStatusService.find(UUID)` | 구현체 | 위와 동일 |
| `ReadStatusService.delete(UUID)` | 인터페이스 선언 | API 스펙에 DELETE /api/readStatuses 엔드포인트 없음 |
| `BasicReadStatusService.delete(UUID)` | 구현체 | 위와 동일 |
| `BinaryContentService.delete(UUID)` | 인터페이스 선언 | 컨트롤러 미호출 (삭제는 다른 서비스에서 직접 Repository 사용) |
| `BasicBinaryContentService.delete(UUID)` | 구현체 | 위와 동일 |
| `UserStatusService.findByUserId(UUID)` | 인터페이스 선언 | 컨트롤러 미호출 (GET /api/users/{userId}/userStatus 없음) |
| `BasicUserStatusService.findByUserId(UUID)` | 구현체 | 위와 동일 |

### 5-2. README6-2.md의 커서 페이지네이션 설명과 실제 코드 불일치

README6-2.md에 커서 기반 페이지네이션 설명이 있지만, 현재 코드는 이미 페이지(number) 기반으로 수정됨.
README6-2.md 내용은 이전 구현에 대한 설명이므로 실제 코드와 불일치함.
단, README는 학습/문서 목적이므로 삭제 대상은 아님.
