# 스프린트 8 심화요구사항 설계서

## 1. 이미지 최적화

### 1-1. 멀티 스테이지 빌드 (slim 이미지)

**현재 문제:** 런타임 스테이지에서 `amazoncorretto:17` (풀 JDK, ~500MB) 사용 중.

**해결 방안:** 런타임 스테이지를 `amazoncorretto:17-alpine` (JRE 포함 경량 이미지, ~180MB)으로 교체.

```
Stage 1 (build): amazoncorretto:17 — Gradle 빌드용 (풀 JDK 필요)
Stage 2 (runtime): amazoncorretto:17-alpine — JAR 실행만 (JRE면 충분)
```

- 태그: `local-slim`
- 예상 결과: 878MB → ~300MB 이하

### 1-2. 레이어 캐시 최적화

**현재 문제:** `COPY . .` 후 빌드 → 소스 코드 1줄만 바꿔도 Gradle 의존성 다시 다운로드.

**해결 방안:** 의존성 파일만 먼저 복사 후 의존성 다운로드, 그 다음 소스 코드 복사 후 빌드.

```dockerfile
# 1) Gradle Wrapper + 빌드 설정 파일만 복사
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# 2) 소스 코드 복사 + 빌드
COPY src ./src
RUN ./gradlew clean build -x test --no-daemon
```

---

## 2. CI 워크플로우 (test.yml)

### 트리거
- `main` 브랜치로 PR 생성/업데이트 시

### Job: test
- **러너:** `ubuntu-latest`
- **Steps:**
  1. 코드 체크아웃 (`actions/checkout@v4`)
  2. JDK 17 설정 (`actions/setup-java@v4` with `distribution: corretto`)
  3. Gradle 캐시 설정 (`actions/cache@v4` — `~/.gradle/caches`, `~/.gradle/wrapper`)
  4. Gradle 테스트 실행 (`./gradlew clean test`)
  5. JaCoCo XML 리포트 → CodeCov 업로드 (`codecov/codecov-action@v4`)
     - `CODECOV_TOKEN` 시크릿 사용
     - `file: build/reports/jacoco/test/jacocoTestReport.xml`

### README 뱃지
- 루트에 `README.md` 생성
- CodeCov 뱃지 URL: `https://codecov.io/gh/Jong-Ho-LEE-75/9-sprint-mission/branch/main/graph/badge.svg`

---

## 3. CD 워크플로우 (deploy.yml)

### 트리거
- `release` 브랜치에 코드 푸시 시

### 필요한 GitHub Secrets/Variables (수동 설정)
- **Secrets:** `AWS_ACCESS_KEY`, `AWS_SECRET_KEY`
- **Variables:** `AWS_REGION`, `ECR_REPOSITORY_URI`, `ECS_CLUSTER`, `ECS_SERVICE`, `ECS_TASK_DEFINITION`

### Job 1: build-and-push
- **러너:** `ubuntu-latest`
- **Steps:**
  1. 코드 체크아웃
  2. AWS CLI 설정 (`aws-actions/configure-aws-credentials@v4`)
     - **리전: `us-east-1`** (Public ECR 접근용)
  3. Public ECR 로그인
     - `aws ecr-public get-login-password --region us-east-1 | docker login --username AWS --password-stdin public.ecr.aws`
  4. Docker 이미지 빌드 및 푸시
     - 플랫폼: x86_64 단일 (멀티 플랫폼 제외)
     - 태그: `${{ vars.ECR_REPOSITORY_URI }}:latest`, `${{ vars.ECR_REPOSITORY_URI }}:${{ github.sha }}`
     - `docker build -t` → `docker push` (2개 태그)

### Job 2: deploy (depends on: build-and-push)
- **러너:** `ubuntu-latest`
- **Steps:**
  1. AWS CLI 설정
     - **리전: `${{ vars.AWS_REGION }}`** (ECS 클러스터 접근용)
  2. 태스크 정의 업데이트
     - 기존 태스크 정의 JSON 조회: `aws ecs describe-task-definition`
     - 컨테이너 이미지를 새 이미지 URI로 교체
     - `aws ecs register-task-definition`으로 새 리비전 등록
  3. 기존 서비스 중단 (프리티어 리소스 절약)
     - `aws ecs update-service --desired-count 0`
  4. ECS 서비스 업데이트
     - 새 태스크 정의로 서비스 업데이트
     - `aws ecs update-service --task-definition <new-revision> --desired-count 1`

---

## 구현 순서

1. Dockerfile 멀티 스테이지 최적화 → 빌드 → 크기 비교 → 커밋
2. CI 워크플로우 (test.yml) 생성 → 커밋
3. README.md + CodeCov 뱃지 추가 → 커밋
4. CD 워크플로우 (deploy.yml) 생성 → 커밋
