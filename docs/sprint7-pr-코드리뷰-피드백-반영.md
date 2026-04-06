# Sprint 7 PR 코드리뷰 피드백 반영 내역

리뷰어: eedys1234
작성일: 2026-04-06

---

## 1. 로그인 보안: USER_NOT_FOUND → 401 INVALID_PASSWORD 통일

**상태: 적용 완료**

### 피드백 내용
로그인 시 사용자를 찾지 못했을 때 `UserNotFoundException(404)`을 던지면 공격자가 해당 계정의 존재 여부를 유추할 수 있으므로, 비밀번호 불일치와 동일하게 `InvalidPasswordException(401)`으로 통일해야 한다.

### 변경 내용
- `BasicAuthService.java`: `UserNotFoundException` → `InvalidPasswordException`으로 변경
- `BasicAuthServiceTest.java`: 테스트 기대값을 `InvalidPasswordException`으로 수정
- `AuthControllerTest.java`: 404/USER_NOT_FOUND → 401/INVALID_PASSWORD로 수정

### 적용 이유
보안 모범 사례(OWASP). 로그인 실패 응답에서 사용자 존재 여부를 노출하면 브루트포스 공격 시 계정 열거(account enumeration)에 악용될 수 있다.

---

## 2. 컨트롤러 테스트에 //given //when //then 주석 추가

**상태: 적용 완료**

### 피드백 내용
컨트롤러 슬라이스 테스트에 BDD 스타일의 `// given`, `// when & then` 주석이 없어 테스트 구조를 파악하기 어렵다.

### 변경 내용
- `AuthControllerTest.java`: 전체 4개 테스트에 주석 추가
- `ChannelControllerTest.java`: 전체 8개 테스트에 주석 추가
- `MessageControllerTest.java`: 전체 7개 테스트에 주석 추가
- `UserControllerTest.java`: 전체 6개 테스트에 주석 추가
- `ReadStatusControllerTest.java`: 전체 4개 테스트에 주석 추가
- `BinaryContentControllerTest.java`: 전체 4개 테스트에 주석 추가

### 적용 이유
서비스 단위 테스트에는 이미 `// given`, `// when`, `// then` 주석이 적용되어 있었으나 컨트롤러 테스트에는 누락되어 있었다. 일관성 있는 테스트 구조 유지를 위해 적용.

---

## 3. MessageRepositoryTest 헬퍼 메서드 추출

**상태: 적용 완료**

### 피드백 내용
`MessageRepositoryTest`에서 `new Message(content, channel, author, List.of())` + `em.persist()` 패턴이 반복적으로 사용되므로 헬퍼 메서드로 추출하면 가독성이 향상된다.

### 변경 내용
- `MessageRepositoryTest.java`: `persistMessage(String content)` 헬퍼 메서드 추출, 4개 테스트에서 활용

### 적용 이유
동일 패턴이 4개 테스트 메서드에서 반복되고 있어 중복 제거와 가독성 향상 효과가 명확하다.

---

## 4. 테스트 변수에 final 키워드 추가

**상태: 미적용**

### 피드백 내용
테스트 메서드 내 지역 변수에 `final` 키워드를 추가하면 불변성을 명시적으로 표현할 수 있다.

### 미적용 이유
- Java 테스트 코드에서 지역 변수에 `final`을 붙이는 것은 선택적 스타일 컨벤션이며, 기존 프로젝트 코드(서비스, 리포지토리 테스트 모두)에서 `final`을 사용하지 않는 것이 일관된 컨벤션이다.
- 프로젝트 전체의 일관성을 유지하기 위해 현재 단계에서는 적용하지 않는다.
- 향후 프로젝트 전체 컨벤션으로 도입하는 것은 별도 논의가 필요하다.

---

## 5. getter 기반 assertion 사용

**상태: 미적용 (해당 없음)**

### 피드백 내용
테스트 assertion에서 하드코딩된 문자열 대신 getter를 통해 검증하면 리팩토링에 안전하다.

### 미적용 이유
- 컨트롤러 슬라이스 테스트는 MockMvc의 `jsonPath`로 HTTP 응답 JSON을 검증하므로, 엔티티나 DTO의 getter를 직접 사용할 수 없다.
- 서비스 단위 테스트에서는 이미 `result.username()`, `result.email()` 등 record의 접근자를 통해 검증하고 있다.
- 컨트롤러 테스트에서 `jsonPath("$.username").value("testuser")`는 API 계약(JSON 필드명 + 값)을 검증하는 것이 목적이므로 하드코딩 문자열이 오히려 적절하다.

---

## 6. AOP 기반 컨트롤러 로깅

**상태: 미적용 (향후 개선 사항)**

### 피드백 내용
현재 컨트롤러에 `@Slf4j`가 없고 직접적인 로깅이 없다. AOP를 활용하여 컨트롤러 레벨의 요청/응답 로깅을 구현하면 좋겠다.

### 미적용 이유
- 현재 `MDCLoggingInterceptor`(HandlerInterceptor)가 모든 요청에 대해 requestId/requestMethod/requestUrl을 MDC에 설정하고 응답 시간과 상태 코드를 로깅하고 있다.
- 각 서비스 계층에 `@Slf4j`가 적용되어 비즈니스 로직 레벨의 로깅이 이미 수행되고 있다.
- AOP 기반 컨트롤러 로깅은 기존 인터셉터 로깅과 중복될 수 있으며, 요청/응답 바디 로깅이 필요한 경우에만 AOP의 이점이 있다.
- 현재 스프린트 범위 외의 기능이므로 향후 개선 사항으로 남긴다.

---

## 요약

| # | 피드백 항목 | 상태 | 비고 |
|---|-----------|------|------|
| 1 | 로그인 보안 통일 (401) | 적용 완료 | 보안 취약점 수정 |
| 2 | //given //when //then 주석 | 적용 완료 | 전체 컨트롤러 테스트 |
| 3 | Message 헬퍼 메서드 추출 | 적용 완료 | MessageRepositoryTest |
| 4 | final 키워드 | 미적용 | 기존 컨벤션과 불일치 |
| 5 | getter 기반 assertion | 미적용 | 컨트롤러 테스트는 jsonPath 사용 |
| 6 | AOP 컨트롤러 로깅 | 미적용 | 기존 인터셉터로 충분, 향후 개선 |
