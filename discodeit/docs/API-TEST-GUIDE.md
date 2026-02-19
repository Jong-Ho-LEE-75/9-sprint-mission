# 📮 API 테스트 가이드

IntelliJ IDEA의 HTTP Client를 사용하여 모든 API를 자동으로 테스트할 수 있습니다.

## 🚀 빠른 시작

### 1. 파일 열기

IntelliJ에서 다음 파일 중 하나를 엽니다:

- **`api-tests-with-env.http`** (권장) - 환경 변수 사용
- **`api-tests.http`** - 기본 버전

### 2. 환경 선택

`api-tests-with-env.http` 사용 시:
- 파일 우측 상단의 환경 드롭다운에서 **`dev`** 선택
- 실제 사용자 ID가 자동으로 적용됩니다

### 3. API 실행

각 요청 옆의 **▶ 버튼** 클릭 또는:
- **Windows/Linux**: `Ctrl` + `Enter`
- **Mac**: `Cmd` + `Enter`

### 4. 결과 확인

- 하단에 응답 결과가 표시됩니다
- JSON 형식으로 자동 포맷팅됩니다
- 에러 발생 시 상태 코드와 메시지 확인 가능

## 📋 테스트 항목

### ✅ 사용자 관리
- [x] 모든 사용자 조회
- [x] 특정 사용자 조회
- [x] 사용자 생성 (JSON)
- [x] 사용자 생성 (프로필 이미지 포함)
- [x] 사용자 정보 수정 (JSON)
- [x] 사용자 정보 수정 (프로필 이미지 포함)
- [x] 사용자 삭제

### ✅ 사용자 상태 관리
- [x] 온라인 상태 업데이트
- [x] 여러 사용자 상태 일괄 업데이트

### ✅ 바이너리 콘텐츠
- [x] 파일 1개 조회
- [x] 파일 여러개 조회

### ✅ 시나리오 테스트
- [x] 사용자 생성 → 조회 → 수정 → 삭제 전체 플로우
- [x] 자동 검증 (응답 상태 코드, 데이터 검증)

### ✅ 에러 케이스
- [x] 중복 이메일 검증
- [x] 존재하지 않는 리소스 조회 (404)

## 🔧 환경 변수

`http-client.env.json` 파일에 정의된 변수들:

```json
{
  "dev": {
    "baseUrl": "http://localhost:8080",
    "user1Id": "한성재 사용자 ID",
    "user1ProfileId": "한성재 프로필 이미지 ID",
    "user2Id": "서현하 사용자 ID",
    ...
  }
}
```

### 사용 예시
```http
GET {{baseUrl}}/users/{{user1Id}}
```

## 💡 동적 변수

IntelliJ HTTP Client가 제공하는 동적 변수:

| 변수 | 설명 | 예시 |
|------|------|------|
| `{{$uuid}}` | 랜덤 UUID | `123e4567-e89b-12d3-a456-426614174000` |
| `{{$timestamp}}` | Unix 타임스탬프 | `1707567890123` |
| `{{$isoTimestamp}}` | ISO 8601 시간 | `2026-02-10T08:23:57Z` |
| `{{$randomInt}}` | 랜덤 정수 (0-1000) | `742` |

### 사용 예시
```http
POST {{baseUrl}}/users
Content-Type: application/json

{
  "username": "user_{{$randomInt}}",
  "email": "user_{{$timestamp}}@test.com",
  "password": "password123"
}
```

## 🧪 자동 검증

응답 검증 스크립트 예시:

```javascript
> {%
    client.test("사용자 조회 성공", function() {
        client.assert(response.status === 200, "상태 코드가 200이어야 함");
        client.assert(response.body.username !== null, "사용자명이 있어야 함");
    });
    client.log("✅ 테스트 통과");
%}
```

## 🎯 시나리오 테스트 실행

전체 시나리오를 순서대로 실행:

1. **사용자 생성** → `newUserId` 변수에 저장
2. **생성한 사용자 조회** → `newUserId` 사용
3. **온라인 상태 업데이트** → `newUserId` 사용
4. **사용자 정보 수정** → `newUserId` 사용
5. **사용자 삭제** → `newUserId` 사용
6. **삭제 확인 (404)** → `newUserId` 사용

각 단계마다 자동으로 검증이 실행됩니다.

## 📊 실행 결과 예시

```
✅ 사용자 생성 완료 - ID: 123e4567-e89b-12d3-a456-426614174000
✅ 사용자 조회 완료 - 이름: 시나리오유저
✅ 온라인 상태 업데이트 완료
✅ 사용자 정보 수정 완료
✅ 사용자 삭제 완료
✅ 삭제 확인 완료 - 404 응답
```

## 🔄 데이터 초기화

테스트 후 데이터 초기화가 필요하면:

1. 애플리케이션 재시작
2. 초기 4명의 사용자만 남음 (한성재, 장현민, 전명훈, 서현하)

## 🆘 문제 해결

### Q: "환경을 선택할 수 없어요"
**A**: `http-client.env.json` 파일이 프로젝트 루트에 있는지 확인하세요.

### Q: "프로필 이미지 업로드가 실패해요"
**A**: `src/main/resources/data/profiles/` 폴더에 이미지 파일이 있는지 확인하세요.

### Q: "중복 이메일 오류가 나요"
**A**: 동적 변수 `{{$randomInt}}`나 `{{$timestamp}}`를 사용해서 고유한 이메일을 생성하세요.

### Q: "404 오류가 나요"
**A**:
- 애플리케이션이 실행 중인지 확인 (`http://localhost:8080`)
- `http-client.env.json`의 ID 값이 최신인지 확인

## 📚 추가 리소스

- [IntelliJ HTTP Client 공식 문서](https://www.jetbrains.com/help/idea/http-client-in-product-code-editor.html)
- [환경 변수 설정 가이드](https://www.jetbrains.com/help/idea/exploring-http-syntax.html#environment-variables)
