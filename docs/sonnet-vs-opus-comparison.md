# Sonnet vs Opus 코드 분석 비교

## 개요

두 모델이 동일한 코드베이스를 API 스펙 v1.2 기준으로 분석하고 불필요한 코드를 정리한 결과를 비교한다.

---

## 1. 분석 범위 비교

| 항목 | Sonnet | Opus |
|------|--------|------|
| API 엔드포인트 매핑 검증 | O (22개 항목) | O (21개 항목) |
| DTO/스키마 필드 검증 | X (언급 없음) | O (18개 DTO 전수 검사) |
| 기본 요구사항 점검 | X (언급 없음) | O (8개 항목 테이블) |
| 심화 요구사항 점검 | X (언급 없음) | O (7개 항목 테이블) |
| 치명적 버그 발견 | O (PageResponse nextCursor 버그) | X (Sonnet이 이미 수정 완료) |

**차이점**: Sonnet은 버그 발견과 수정에 초점을 맞추었고, Opus는 이미 수정된 상태에서 기본/심화 요구사항 전체에 대한 체계적 검증에 초점을 맞추었다.

---

## 2. 발견한 불필요 코드 비교

### 2-1. 두 모델 모두 발견하고 삭제한 항목 (공통)

| 대상 | Sonnet | Opus |
|------|--------|------|
| `UserController.getUserStatusByUserId()` (GET 엔드포인트) | O - 삭제 | 이미 삭제됨 (Sonnet이 먼저 처리) |
| `UserApi.getUserStatusByUserId()` (Swagger 인터페이스) | O - 삭제 | 이미 삭제됨 |
| `MessageService.find(UUID)` + 구현체 | O - 삭제 | 이미 삭제됨 |
| `ChannelService.find(UUID)` + 구현체 | O - 삭제 | 이미 삭제됨 |
| `UserStatusCreateRequest.java` (미사용 DTO) | O - 파일 삭제 | 이미 삭제됨 |
| cursor 관련 코드 (MessageRepository, MessageController 등) | O - 삭제 | 이미 삭제됨 |
| `MessageRepository.findAllByChannelIdBeforeCursor()` | O - 삭제 | 이미 삭제됨 |
| `MessageRepository.deleteAllByChannel_Id()` | O - 삭제 | 이미 삭제됨 |
| `ReadStatusRepository.deleteAllByChannel_Id()` | O - 삭제 | 이미 삭제됨 |

### 2-2. Opus만 추가로 발견하고 삭제한 항목

| 대상 | 이유 |
|------|------|
| `UserService.find(UUID)` + `BasicUserService.find(UUID)` | API 스펙에 GET /api/users/{userId} 엔드포인트 없음, 컨트롤러 미호출 |
| `ReadStatusService.find(UUID)` + `BasicReadStatusService.find(UUID)` | 어디서도 호출되지 않음 |
| `ReadStatusService.delete(UUID)` + `BasicReadStatusService.delete(UUID)` | API 스펙에 DELETE /api/readStatuses 엔드포인트 없음 |
| `BinaryContentService.delete(UUID)` + `BasicBinaryContentService.delete(UUID)` | 컨트롤러 미호출 (삭제는 다른 서비스에서 Repository 직접 사용) |
| `UserStatusService.findByUserId(UUID)` + `BasicUserStatusService.findByUserId(UUID)` | 컨트롤러 미호출 (GET 엔드포인트가 API 스펙에 없음) |

### 2-3. Sonnet만 발견했지만 Opus가 다시 확인할 필요 없었던 항목

| 대상 | 설명 |
|------|------|
| PageResponse `nextCursor` -> `number` 버그 수정 | Sonnet이 치명적 버그를 발견하고 수정 완료 |
| DB FK 제약조건 불일치 (NO ACTION -> CASCADE) | Sonnet이 DB ALTER TABLE로 수정 완료 |

---

## 3. 분석 접근 방식 차이

| 관점 | Sonnet | Opus |
|------|--------|------|
| **분석 방향** | 동작 테스트(curl) 중심 + 버그 수정 | 정적 코드 분석 + 스펙 대조 중심 |
| **버그 발견** | PageResponse 필드 불일치, DB FK 제약조건 불일치 등 런타임 버그 발견 | 미사용 코드 정적 분석에 집중 |
| **불필요 코드 기준** | "API 스펙에 없는 엔드포인트", "사용되지 않는 Repository 메서드" | "컨트롤러에서 호출되지 않는 Service 메서드" (더 넓은 범위) |
| **보고서 구조** | 버그 + 삭제 대상 위주 | 요구사항 준수 검증 + 삭제 대상 |

---

## 4. 삭제 코드 수량 비교

| 구분 | Sonnet이 삭제 | Opus가 추가 삭제 | 합계 |
|------|-------------|-----------------|------|
| 컨트롤러 메서드 | 1개 (GET userStatus) | 0개 | 1개 |
| API 인터페이스 메서드 | 1개 | 0개 | 1개 |
| Service 인터페이스 메서드 | 2개 (Message.find, Channel.find) | 5개 (User.find, ReadStatus.find, ReadStatus.delete, BinaryContent.delete, UserStatus.findByUserId) | 7개 |
| Service 구현체 메서드 | 2개 | 5개 | 7개 |
| Repository 메서드 | 3개 | 0개 | 3개 |
| DTO 파일 | 1개 (UserStatusCreateRequest) | 0개 | 1개 |
| **총계** | **10개** | **10개** | **20개** |

---

## 5. 결론

### Sonnet의 강점
- **런타임 버그 발견 능력**: curl 테스트를 통해 PageResponse 필드 불일치, DB FK 제약조건 불일치 등 실제 동작에 영향을 주는 치명적 버그를 발견하고 수정
- **실행 기반 검증**: 서버를 실행하고 직접 API를 호출하여 동작을 검증

### Opus의 강점
- **체계적 정적 분석**: 모든 Service 인터페이스 메서드를 컨트롤러 호출 여부로 역추적하여, Sonnet이 놓친 5개의 미사용 메서드를 추가 발견
- **요구사항 대조 검증**: 기본/심화 요구사항을 항목별로 체계적으로 검증하여 누락 없는 보고서 작성
- **DTO 필드 전수 검사**: 18개 DTO의 모든 필드를 API 스펙과 1:1 대조 검증

### 종합 평가
두 모델의 분석은 **상호 보완적**이었다.
- Sonnet이 먼저 실행하여 **런타임 버그를 수정**하고 기본적인 불필요 코드를 정리
- Opus가 이후 정적 분석으로 **Sonnet이 놓친 미사용 Service 메서드 5개를 추가 정리**
- 합계 20개의 불필요 항목이 제거되어 코드가 API 스펙 v1.2에 정확히 부합하는 상태가 됨
