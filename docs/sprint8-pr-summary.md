# 스프린트 미션 8 - PR 요약

## 개요

Discord 유사 메시징 플랫폼(Discodeit)의 프로덕션 배포 준비.
애플리케이션 컨테이너화(Docker), AWS S3 파일 저장소 고도화, AWS 인프라(RDS/ECR/ECS) 배포를 구현했습니다.

---

## 기본 요구사항 체크리스트

### 1. 애플리케이션 컨테이너화

#### Dockerfile 작성
- [x] Amazon Corretto 17 이미지를 베이스 이미지로 사용
- [x] 작업 디렉토리 설정 (`/app`)
- [x] 프로젝트 파일을 컨테이너로 복사 (`.dockerignore`로 불필요한 파일 제외)
- [x] Gradle Wrapper를 사용하여 애플리케이션 빌드
- [x] 80 포트 노출
- [x] 프로젝트 정보를 환경 변수로 설정 (`PROJECT_NAME=discodeit`, `PROJECT_VERSION=1.2-M8`)
- [x] JVM 옵션을 환경 변수로 설정 (`JVM_OPTS`, 기본값 빈 문자열)
- [x] 애플리케이션 실행 명령어 설정 (환경변수로 정의한 프로젝트 정보 활용)

#### 이미지 빌드 및 실행 테스트
- [x] Docker 이미지 빌드 및 태그(`local`) 지정
- [x] `prod` 프로필로 컨테이너 실행
- [x] 로컬 PostgreSQL 서버 활용
- [x] `http://localhost:8081`로 접속 가능하도록 포트 매핑

#### Docker Compose 구성
- [x] 애플리케이션과 PostgreSQL 서비스 포함
- [x] `.env` 파일 활용 (형상관리에서 제외)
- [x] 애플리케이션 서비스를 로컬 Dockerfile에서 빌드하도록 구성
- [x] 애플리케이션 볼륨 구성 (BinaryContentStorage 데이터 유지)
- [x] PostgreSQL 볼륨 구성 (컨테이너 재시작 시 데이터 유지)
- [x] PostgreSQL 서비스 실행 후 `schema.sql` 자동 실행되도록 구성
- [x] 서비스 간 의존성 설정 (`depends_on` + `service_healthy`)
- [x] 필요한 포트 매핑 구성
- [x] Docker Compose로 서비스 시작 및 테스트 (`--build` 플래그)

### 2. BinaryContentStorage 고도화 (AWS S3)

#### AWS S3 버킷 구성
- [x] S3 버킷 생성 (`discodeit-binary-content-storage-ljh-633267`)
- [x] 퍼블릭 액세스 차단 설정 활성화
- [x] 버전 관리 비활성화

#### AWS S3 접근을 위한 IAM 구성
- [x] IAM 사용자(`discodeit`) 생성
- [x] `AmazonS3FullAccess` 권한 할당
- [x] 엑세스 키 생성 및 `.env` 파일에 추가

#### AWS S3 테스트
- [x] AWS S3 SDK 의존성 추가 (`software.amazon.awssdk:s3:2.31.7`)
- [x] `AWSS3Test` 클래스 작성 (업로드, 다운로드, PresignedUrl 생성 테스트)
- [x] Properties 클래스를 활용해 `.env`에서 AWS 정보 로드

#### S3BinaryContentStorage 구현
- [x] `@ConditionalOnProperty(name="discodeit.storage.type", havingValue="s3")`로 Bean 등록
- [x] `S3BinaryContentStorageTest` 작성
- [x] `application.yaml` 수정 (스토리지 설정 유연화)
  ```yaml
  discodeit:
    storage:
      type: ${STORAGE_TYPE:local}
      local:
        root-path: ${STORAGE_LOCAL_ROOT_PATH:.discodeit/storage}
      s3:
        access-key: ${AWS_S3_ACCESS_KEY}
        secret-key: ${AWS_S3_SECRET_KEY}
        region: ${AWS_S3_REGION}
        bucket: ${AWS_S3_BUCKET}
        presigned-url-expiration: ${AWS_S3_PRESIGNED_URL_EXPIRATION:600}
  ```
- [x] AWS 관련 정보 형상관리 금지 (`.env` 파일에서 임포트)
- [x] Docker Compose에서도 설정 주입 가능하도록 수정
- [x] `download` 메소드 PresignedUrl → 리다이렉트 방식 구현

### 3. AWS를 활용한 배포 (AWS RDS, ECR, ECS)

#### AWS RDS 구성
- [x] PostgreSQL 17.2 인스턴스 생성 (`discodeit-db`, db.t4g.micro)
- [x] 프리 티어 템플릿 사용
- [x] 퍼블릭 액세스: 아니오
- [x] SSH 터널링을 통한 RDS 접근 (EC2 경유)
- [x] DataGrip으로 연결 후 DB 초기화 (사용자 생성, 데이터베이스 생성, schema.sql 실행)
- [x] SSH 터널링용 EC2 인스턴스 삭제 완료

#### AWS ECR 구성
- [x] 퍼블릭 레포지토리(`discodeit`) 생성
- [x] AWS CLI 설치 및 `aws configure` 실행
- [x] `AmazonElasticContainerRegistryPublicFullAccess` 권한 부여
- [x] Docker 클라이언트 인증
- [x] 멀티플랫폼 이미지 빌드 및 push (`linux/amd64`, `linux/arm64`)
- [x] 태그: `latest`, `1.2-M8`

#### AWS ECS 구성
- [x] 배포 환경 변수 파일(`discodeit.env`) 작성 및 S3 업로드
- [x] ECS 클러스터 생성 (`discodeit-cluster`, EC2 인스턴스 방식)
- [x] 태스크 정의 (`discodeit-task`, bridge 네트워크, CPU 0.25 vCPU, 메모리 0.8 GB)
- [x] 서비스 생성 (`discodeit-service`, 원하는 태스크 1)
- [x] EC2 보안 그룹 인바운드 규칙 설정 (HTTP 80, Anywhere-IPv4)
- [x] 태스크 실행 역할에 S3/CloudWatch 권한 추가

---

## 상세 구현 내용

### 1. 애플리케이션 컨테이너화

#### Dockerfile (멀티스테이지 빌드)

| 항목 | 설정값 |
|------|--------|
| 베이스 이미지 | `amazoncorretto:17` |
| 작업 디렉토리 | `/app` |
| 빌드 방식 | 멀티스테이지 (빌드 → 실행 분리) |
| 포트 | 80 |
| 환경변수 | `PROJECT_NAME=discodeit`, `PROJECT_VERSION=1.2-M8`, `JVM_OPTS=""` |
| 실행 | `sh -c "java $JVM_OPTS -jar app.jar"` (환경변수 확장) |

- `.dockerignore`로 `.gradle/`, `build/`, `.idea/`, `.git/`, `.env`, `docs/`, `frontend/node_modules/` 등 제외
- `-x test` 옵션으로 Docker 빌드 시 테스트 스킵 (H2 환경 없음)

#### Docker Compose

| 서비스 | 이미지 | 포트 | 볼륨 |
|--------|--------|------|------|
| `app` | 로컬 Dockerfile 빌드 | `${APP_PORT:-8080}:80` | `app-storage:/app/.discodeit/storage` |
| `db` | `postgres:17` | `${DB_PORT:-5432}:5432` | `db-data:/var/lib/postgresql/data` + `schema.sql` |

- `depends_on: condition: service_healthy` — PostgreSQL 헬스체크 통과 후 앱 시작
- `schema.sql` → `docker-entrypoint-initdb.d/` — 첫 실행 시 자동 스키마 생성
- `.env` 파일로 모든 환경변수 주입 (형상관리 제외, `.env.example` 제공)

---

### 2. S3BinaryContentStorage

#### 클래스 구조

```
<<interface>> BinaryContentStorage
  +UUID put(UUID, byte[])
  +InputStream get(UUID)
  +ResponseEntity<?> download(BinaryContentDto)
  +void delete(UUID)
        △
        │ implements
  ┌─────┴──────┐
  │            │
LocalBinary   S3Binary
ContentStorage ContentStorage
(type=local)   (type=s3)
```

#### S3BinaryContentStorage 주요 특징

| 메서드 | 동작 |
|--------|------|
| `put(UUID, byte[])` | `PutObjectRequest` → S3에 업로드 |
| `get(UUID)` | `GetObjectRequest` → S3에서 다운로드 → `InputStream` 반환 |
| `download(BinaryContentDto)` | PresignedUrl 생성 → `302 Found` 리다이렉트 |
| `delete(UUID)` | `DeleteObjectRequest` → S3에서 삭제 |

- `@ConditionalOnProperty(name = "discodeit.storage.type", havingValue = "s3")`로 조건부 Bean 등록
- `@Value`로 `application.yaml`의 S3 설정 주입
- `StaticCredentialsProvider`로 S3Client 생성
- PresignedUrl 만료 시간: 기본 600초 (10분), 환경변수로 설정 가능

#### 테스트

| 테스트 클래스 | 테스트 내용 |
|-------------|-----------|
| `AWSS3Test` | S3 API 직접 테스트 (업로드, 다운로드, PresignedUrl 생성) |
| `S3BinaryContentStorageTest` | 구현체의 put, get, download, delete 메서드 테스트 |

- `.env` 파일의 AWS 정보를 `Properties`로 로드하여 테스트 실행
- Spring 컨텍스트 없이 독립 실행 가능

---

### 3. 설정 파일 환경변수화

#### application.yaml 변경사항

```yaml
# .env 파일 자동 임포트
spring:
  config:
    import: optional:file:.env[.properties]

# 스토리지 설정 환경변수화
discodeit:
  storage:
    type: ${STORAGE_TYPE:local}
    s3:
      access-key: ${AWS_S3_ACCESS_KEY:}
      secret-key: ${AWS_S3_SECRET_KEY:}
      region: ${AWS_S3_REGION:ap-northeast-2}
      bucket: ${AWS_S3_BUCKET:}
      presigned-url-expiration: ${AWS_S3_PRESIGNED_URL_EXPIRATION:600}
```

#### application-prod.yaml 변경사항

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}

server:
  port: 80
```

---

### 4. AWS 인프라 구성

#### 생성된 리소스 요약

| 리소스 | 이름/식별자 | 비고 |
|--------|------------|------|
| S3 버킷 | `discodeit-binary-content-storage-ljh-633267` | 바이너리 저장소 |
| S3 버킷 | `discodeit-ecs-config-ljh` | ECS 환경변수 파일용 |
| IAM 사용자 | `discodeit` | S3FullAccess + ECR 권한 |
| RDS | `discodeit-db` (db.t4g.micro, PostgreSQL 17) | 프리티어 |
| ECR 퍼블릭 | `public.ecr.aws/q1y9z3p9/discodeit` | latest, 1.2-M8 태그 |
| ECS 클러스터 | `discodeit-cluster` | EC2 방식 |
| ECS 서비스 | `discodeit-service` | 태스크 1개 |
| ECS 태스크 정의 | `discodeit-task` (rev 3) | bridge 네트워크 |
| EC2 (ECS) | t3.micro | 컨테이너 인스턴스 |

#### 배포 아키텍처

```
사용자 → EC2 퍼블릭 IP:80
           │
           ▼
        ECS 태스크 (discodeit-app 컨테이너)
           │                    │
           ▼                    ▼
        RDS PostgreSQL     S3 버킷
        (discodeit-db)     (바이너리 저장소)
```

---

## 주요 커밋 내역

### 스프린트 8 신규 커밋

| 커밋 | 설명 |
|------|------|
| `a4e4428f` | 프로젝트 버전 1.2-M8로 변경 |
| `970eff88` | Spring Boot Admin 관련 코드 전체 삭제 |
| `566508b1` | s8-v1-base 코드 개선사항 반영 |
| `a5118aeb` | 설정 파일 환경변수화 및 S3 스토리지 설정 추가 |
| `03a1ea53` | 애플리케이션 컨테이너화 (Dockerfile, Docker Compose) |
| `6fed20dc` | S3 BinaryContentStorage 구현 및 테스트 추가 |
| `699b071b` | ECS 메모리 최적화를 위한 prod 설정 추가 |

### 이전 스프린트(7) 포함 커밋

| 구분 | 커밋 수 | 내용 |
|------|---------|------|
| 프로파일/로깅/예외처리 | 6 | 설정 분리, Logback, MDC, 커스텀 예외 계층, DTO 유효성 검사 |
| 테스트 | 12 | 서비스 단위(73), 리포지토리 슬라이스(18), 컨트롤러 슬라이스(22), 통합(10) = 총 123개 |
| 코드 정리 | 4 | 불필요한 파일 정리, 주석 추가, 코드리뷰 반영 |

---

## 테스트 결과

기존 테스트 123개 + S3 관련 테스트 추가

| 구분 | 테스트 수 | 비고 |
|------|----------|------|
| 서비스 단위 테스트 | 73 | BDDMockito |
| 리포지토리 슬라이스 | 18 | @DataJpaTest + H2 |
| 컨트롤러 슬라이스 | 22 | @WebMvcTest + MockMvc |
| 통합 테스트 | 10 | @SpringBootTest + H2 |
| S3 테스트 | 추가 | AWSS3Test + S3BinaryContentStorageTest |

---

## .env 파일 (PR 첨부용, 키 값 제외)

```properties
# === Database ===
POSTGRES_DB=discodeit
POSTGRES_USER=discodeit_user
POSTGRES_PASSWORD=discodeit1234

# === Application ===
APP_PORT=8080
STORAGE_TYPE=local
STORAGE_LOCAL_ROOT_PATH=.discodeit/storage

# === AWS S3 (STORAGE_TYPE=s3 일 때 필요) ===
AWS_S3_ACCESS_KEY=<제외>
AWS_S3_SECRET_KEY=<제외>
AWS_S3_REGION=ap-northeast-2
AWS_S3_BUCKET=discodeit-binary-content-storage-ljh-633267
AWS_S3_PRESIGNED_URL_EXPIRATION=600
```

---

## ECS 메모리 최적화 (OOM 해결)

초기 배포 시 t3.micro(1GB RAM)에서 OOM이 발생하여 다음과 같이 최적화했습니다:

| 항목 | 변경 내용 |
|------|----------|
| JVM Heap | -Xmx256m -Xms128m |
| Metaspace | -XX:MaxMetaspaceSize=128m (기본 무제한 → 제한) |
| Code Cache | -XX:ReservedCodeCacheSize=48m |
| 스레드 스택 | -Xss512k (기본 1MB → 512KB) |
| GC | -XX:+UseSerialGC (단일 스레드) |
| Tomcat 스레드 | max 200 → 20 |
| Hibernate 쿼리 캐시 | 2048 → 64 |
| JMX | 비활성화 |
| 빈 초기화 | 지연 초기화 |

**결과**: OOM ��이 기동 성공 (154초), `http://3.39.195.223` 접속 확인 완료

### 참고
- **EC2 인스턴스 타입**: 요구사항은 t2.micro이나 프리티어 제약으로 t3.micro 사용 (RAM 동일 1GB)
- **기동 시간**: 메모리 제한 환경에서 154초로 느리지만 정상 동작

---

## 변경 파일 요약

### 신규 파일
- `Dockerfile` — 멀티스테이지 빌드 설정
- `.dockerignore` — Docker 빌드 제외 파일
- `docker-compose.yml` — app + PostgreSQL 서비스 구성
- `.env.example` — 환경변수 템플릿
- `src/main/java/.../storage/s3/S3BinaryContentStorage.java` — S3 저장소 구현체
- `src/test/java/.../storage/s3/AWSS3Test.java` — S3 API 직접 테스트
- `src/test/java/.../storage/s3/S3BinaryContentStorageTest.java` — S3 저장소 테스트

### 수정 파일
- `build.gradle` — AWS S3 SDK 의존성 추가, 프로젝트 버전 1.2-M8
- `src/main/resources/application.yaml` — `.env` 임포트, S3 설정 추가
- `src/main/resources/application-prod.yaml` — DB 환경변수화, 포트 80
- `.gitignore` — `.env`, `discodeit.env` 추가

### 삭제 파일
- `admin/` 모듈 전체 — Spring Boot Admin 서버 코드 삭제 (스프린트 8 범위 외)
