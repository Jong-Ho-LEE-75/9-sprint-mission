# 스프린트 8 기본요구사항 코드 리뷰

이 문서는 스프린트 8 기본요구사항 3가지(컨테이너화, S3 스토리지, AWS 배포)에 대해
**구현된 코드를 한 줄씩 설명**합니다. 초보자도 "왜 이렇게 작성했는지"를 이해할 수 있도록 작성했습니다.

---

## 목차

1. [요구사항 1: 애플리케이션 컨테이너화](#1-애플리케이션-컨테이너화)
   - [1.1 Dockerfile](#11-dockerfile)
   - [1.2 .dockerignore](#12-dockerignore)
   - [1.3 docker-compose.yml](#13-docker-composeyml)
   - [1.4 .env.example](#14-envexample)
2. [요구사항 2: BinaryContentStorage 고도화 (AWS S3)](#2-binarycontentstorage-고도화-aws-s3)
   - [2.1 build.gradle S3 SDK 의존성](#21-buildgradle-s3-sdk-의존성)
   - [2.2 application.yaml S3 설정](#22-applicationyaml-s3-설정)
   - [2.3 S3BinaryContentStorage 구현체](#23-s3binarycontentstorage-구현체)
   - [2.4 AWSS3Test (S3 API 직접 테스트)](#24-awss3test-s3-api-직접-테스트)
   - [2.5 S3BinaryContentStorageTest (구현체 테스트)](#25-s3binarycontentstoragetest-구현체-테스트)
3. [요구사항 3: AWS를 활용한 배포](#3-aws를-활용한-배포)
   - [3.1 application-prod.yaml 운영 설정](#31-application-prodyaml-운영-설정)
   - [3.2 ECS 메모리 최적화](#32-ecs-메모리-최적화)
4. [요구사항 체크리스트 대조 결과](#4-요구사항-체크리스트-대조-결과)

---

## 1. 애플리케이션 컨테이너화

### 1.1 Dockerfile

> 파일 위치: `Dockerfile` (프로젝트 루트)

```dockerfile
# Stage 1: 빌드
FROM amazoncorretto:17 AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew clean build -x test

# Stage 2: 실행
FROM amazoncorretto:17
WORKDIR /app

ENV PROJECT_NAME=discodeit
ENV PROJECT_VERSION=1.2-M8
ENV JVM_OPTS=""

COPY --from=build /app/build/libs/${PROJECT_NAME}-${PROJECT_VERSION}.jar app.jar

EXPOSE 80

ENTRYPOINT ["sh", "-c", "java $JVM_OPTS -jar app.jar"]
```

#### 일처리 순서

```
[1] 빌드 스테이지에서 소스코드를 복사하고 JAR 파일을 생성한다
[2] 실행 스테이지에서 JAR 파일만 복사해서 가벼운 이미지를 만든다
[3] 컨테이너가 시작되면 java 명령으로 JAR를 실행한다
```

#### 코드 상세 설명

**Stage 1: 빌드 단계**

| 줄 | 코드 | 설명 |
|---|------|------|
| 2 | `FROM amazoncorretto:17 AS build` | Amazon Corretto 17 JDK를 베이스 이미지로 사용합니다. `AS build`는 이 스테이지에 이름을 붙여서 나중에 참조할 수 있게 합니다. 요구사항에서 Amazon Corretto 17을 지정했습니다. |
| 3 | `WORKDIR /app` | 컨테이너 안의 작업 디렉토리를 `/app`으로 설정합니다. 이후 모든 명령은 이 디렉토리에서 실행됩니다. |
| 4 | `COPY . .` | 현재 디렉토리(프로젝트 루트)의 모든 파일을 컨테이너의 `/app`으로 복사합니다. `.dockerignore`에 정의된 파일은 제외됩니다. |
| 5 | `RUN chmod +x gradlew && ./gradlew clean build -x test` | `chmod +x`로 Gradle Wrapper에 실행 권한을 부여합니다 (Linux에서는 파일 권한이 없을 수 있음). `clean build`로 JAR 파일을 생성하고, `-x test`로 테스트는 건너뜁니다 (Docker 빌드 환경에는 H2 DB가 없으므로). |

**Stage 2: 실행 단계**

| 줄 | 코드 | 설명 |
|---|------|------|
| 8 | `FROM amazoncorretto:17` | 새로운 깨끗한 이미지에서 시작합니다. 빌드 도구(Gradle, 소스코드)는 포함하지 않아 이미지 크기가 줄어듭니다. |
| 9 | `WORKDIR /app` | 실행 스테이지의 작업 디렉토리도 `/app`으로 설정합니다. |
| 11 | `ENV PROJECT_NAME=discodeit` | 환경변수 `PROJECT_NAME`을 `discodeit`으로 설정합니다. JAR 파일 이름을 추론하는 데 사용됩니다. |
| 12 | `ENV PROJECT_VERSION=1.2-M8` | 프로젝트 버전입니다. `build.gradle`의 `version = '1.2-M8'`과 일치해야 합니다. |
| 13 | `ENV JVM_OPTS=""` | JVM 옵션 환경변수입니다. 기본값은 빈 문자열이고, 실행 시 `-Xmx256m` 등을 전달하여 메모리를 제한할 수 있습니다. |
| 15 | `COPY --from=build /app/build/libs/...jar app.jar` | Stage 1(build)에서 생성된 JAR 파일만 복사합니다. `${PROJECT_NAME}-${PROJECT_VERSION}.jar` → `discodeit-1.2-M8.jar`를 `app.jar`라는 단순한 이름으로 복사합니다. |
| 17 | `EXPOSE 80` | 컨테이너가 80번 포트를 사용한다고 선언합니다. 실제 포트 개방은 `docker run -p` 옵션에서 합니다. |
| 19 | `ENTRYPOINT ["sh", "-c", "java $JVM_OPTS -jar app.jar"]` | 컨테이너가 시작될 때 실행할 명령입니다. `sh -c`를 사용하는 이유는 `$JVM_OPTS` 환경변수를 쉘이 해석해야 하기 때문입니다. exec form(`["java", ...]`)으로는 환경변수 확장이 불가능합니다. |

#### 왜 멀티스테이지 빌드인가?

```
단일 스테이지: Gradle + 소스코드 + JAR = ~800MB 이미지
멀티스테이지: JDK + JAR만 = ~400MB 이미지 (약 50% 절감)
```

빌드에 필요한 도구(Gradle, 소스코드)는 실행 시에는 불필요하므로, 최종 이미지에서 제거합니다.

#### 빌드 및 실행 명령

```bash
# 이미지 빌드 (태그: local)
docker build -t discodeit:local .

# 컨테이너 실행 (prod 프로필, 로컬 DB, 8081 포트)
docker run --rm -p 8081:80 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/discodeit \
  -e SPRING_DATASOURCE_USERNAME=discodeit_user \
  -e SPRING_DATASOURCE_PASSWORD=discodeit1234 \
  discodeit:local
```

---

### 1.2 .dockerignore

> 파일 위치: `.dockerignore` (프로젝트 루트)

```
.gradle/        # Gradle 캐시 (빌드 시 새로 생성됨)
build/          # 기존 빌드 결과물 (컨테이너에서 새로 빌드)
.idea/          # IntelliJ 설정 파일
*.iml           # IntelliJ 모듈 파일
.DS_Store       # macOS 메타데이터
.git/           # Git 히스토리 (빌드에 불필요, 용량 큼)
.gitignore      # Git 설정 파일
.logs/          # 로그 파일
.discodeit-storage/  # 로컬 파일 저장소 데이터
.discodeit/     # 로컬 설정
.test-storage/  # 테스트용 저장소
.env            # 환경변수 파일 (보안상 제외, 중요!)
docs/           # 문서 파일 (빌드에 불필요)
frontend/node_modules/  # Node 패키지 (빌드에 불필요)
frontend/dist/  # 프론트엔드 빌드 결과물
CLAUDE.md       # Claude 설정
.claude/        # Claude 설정
```

#### 왜 .dockerignore가 필요한가?

`COPY . .` 명령이 모든 파일을 복사하는데, `.dockerignore`에 명시된 파일은 제외됩니다.
- **보안**: `.env` 파일(AWS 키 포함)이 이미지에 포함되는 것을 방지
- **속도**: `.git/`, `node_modules/` 등 불필요한 대용량 파일 제외로 빌드 시간 단축
- **크기**: 최종 이미지 크기 감소

---

### 1.3 docker-compose.yml

> 파일 위치: `docker-compose.yml` (프로젝트 루트)

```yaml
services:
  app:
    build: .
    ports:
      - "${APP_PORT:-8080}:80"
    depends_on:
      db:
        condition: service_healthy
    volumes:
      - app-storage:/app/.discodeit/storage
    env_file:
      - .env
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/${POSTGRES_DB:-discodeit}
      - SPRING_DATASOURCE_USERNAME=${POSTGRES_USER:-discodeit_user}
      - SPRING_DATASOURCE_PASSWORD=${POSTGRES_PASSWORD:-discodeit1234}

  db:
    image: postgres:17
    environment:
      - POSTGRES_DB=${POSTGRES_DB:-discodeit}
      - POSTGRES_USER=${POSTGRES_USER:-discodeit_user}
      - POSTGRES_PASSWORD=${POSTGRES_PASSWORD:-discodeit1234}
    volumes:
      - db-data:/var/lib/postgresql/data
      - ./src/main/resources/schema.sql:/docker-entrypoint-initdb.d/schema.sql
    ports:
      - "${DB_PORT:-5432}:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-discodeit_user} -d ${POSTGRES_DB:-discodeit}"]
      interval: 5s
      timeout: 5s
      retries: 5

volumes:
  app-storage:
  db-data:
```

#### 일처리 순서

```
[1] docker compose up --build 실행
[2] Dockerfile로 app 이미지를 빌드한다
[3] PostgreSQL 17 이미지를 다운로드한다
[4] db 서비스가 먼저 시작되고, 첫 실행 시 schema.sql을 자동 실행한다
[5] db의 healthcheck가 통과되면 (pg_isready 성공)
[6] app 서비스가 시작된다 (prod 프로필, DB 연결)
[7] localhost:8080으로 접속 가능
```

#### 코드 상세 설명

**app 서비스**

| 설정 | 코드 | 설명 |
|------|------|------|
| 빌드 | `build: .` | 현재 디렉토리의 `Dockerfile`을 사용해서 이미지를 빌드합니다. |
| 포트 | `"${APP_PORT:-8080}:80"` | 호스트의 8080 포트를 컨테이너의 80 포트에 매핑합니다. `.env`에서 `APP_PORT`를 변경할 수 있고, 기본값은 8080입니다. `:-` 문법은 "값이 없으면 기본값 사용"을 의미합니다. |
| 의존성 | `depends_on: db: condition: service_healthy` | db 서비스의 healthcheck가 성공한 후에만 app을 시작합니다. 단순 `depends_on: db`는 "시작만 됨"을 보장하지만, `condition: service_healthy`는 "준비 완료"를 보장합니다. |
| 볼륨 | `app-storage:/app/.discodeit/storage` | 컨테이너 재시작 시에도 업로드된 파일(BinaryContent)이 유지되도록 Docker 볼륨을 마운트합니다. |
| 환경변수 | `env_file: .env` | `.env` 파일의 모든 변수를 컨테이너에 주입합니다. AWS S3 키 등이 여기에 포함됩니다. |
| 환경변수 | `SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/...` | Docker Compose 내부 네트워크에서 db 서비스는 `db`라는 호스트명으로 접근합니다. `localhost`가 아닌 `db`를 사용하는 것이 핵심입니다. |

**db 서비스**

| 설정 | 코드 | 설명 |
|------|------|------|
| 이미지 | `postgres:17` | 공식 PostgreSQL 17 이미지를 사용합니다. |
| 환경변수 | `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | PostgreSQL 컨테이너가 시작될 때 자동으로 데이터베이스와 사용자를 생성합니다. |
| 볼륨(데이터) | `db-data:/var/lib/postgresql/data` | PostgreSQL 데이터를 Docker 볼륨에 저장합니다. 컨테이너를 삭제해도 데이터가 유지됩니다. |
| 볼륨(스키마) | `./src/.../schema.sql:/docker-entrypoint-initdb.d/schema.sql` | PostgreSQL 컨테이너는 `/docker-entrypoint-initdb.d/` 디렉토리의 SQL 파일을 첫 실행 시 자동으로 실행합니다. 이렇게 하면 수동으로 schema.sql을 실행할 필요가 없습니다. |
| 헬스체크 | `pg_isready -U ... -d ...` | 5초 간격으로 PostgreSQL이 준비되었는지 확인합니다. 5회 실패하면 unhealthy로 판정됩니다. |

**volumes 섹션**

```yaml
volumes:
  app-storage:  # 앱 파일 저장소 볼륨 (Docker가 관리)
  db-data:      # PostgreSQL 데이터 볼륨 (Docker가 관리)
```

볼륨을 선언하면 Docker가 자동으로 관리합니다. `docker compose down`으로 서비스를 중지해도 볼륨은 유지되고, `docker compose down -v`를 해야 볼륨이 삭제됩니다.

#### 실행 명령

```bash
# 서비스 시작 (이미지 빌드 포함)
docker compose up --build

# 백그라운드 실행
docker compose up --build -d

# 중지
docker compose down

# 중지 + 볼륨 삭제 (데이터 초기화)
docker compose down -v
```

---

### 1.4 .env.example

> 파일 위치: `.env.example` (프로젝트 루트)

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
AWS_S3_ACCESS_KEY=
AWS_S3_SECRET_KEY=
AWS_S3_REGION=ap-northeast-2
AWS_S3_BUCKET=
AWS_S3_PRESIGNED_URL_EXPIRATION=600
```

#### 왜 .env.example이 필요한가?

`.env` 파일은 AWS 키 등 민감한 정보를 포함하므로 `.gitignore`에 추가하여 형상관리에서 제외합니다.
하지만 다른 개발자가 어떤 환경변수를 설정해야 하는지 알 수 없으므로, 값이 비어있는 템플릿 파일(`.env.example`)을 제공합니다.

```bash
# 사용법: .env.example을 복사해서 .env를 만들고 값을 채운다
cp .env.example .env
# .env 파일을 편집하여 실제 값 입력
```

---

## 2. BinaryContentStorage 고도화 (AWS S3)

### 2.1 build.gradle S3 SDK 의존성

> 파일 위치: `build.gradle` 34행

```gradle
implementation 'software.amazon.awssdk:s3:2.31.7'
```

AWS SDK v2의 S3 모듈을 추가합니다. 요구사항에서 정확히 이 버전(`2.31.7`)을 지정했습니다.
이 의존성이 있어야 `S3Client`, `S3Presigner` 등의 클래스를 사용할 수 있습니다.

---

### 2.2 application.yaml S3 설정

> 파일 위치: `src/main/resources/application.yaml` 22~33행

```yaml
discodeit:
  storage:
    type: ${STORAGE_TYPE:local}  # local | s3 (환경변수, 기본값: local)
    local:
      root-path: ${STORAGE_LOCAL_ROOT_PATH:.discodeit/storage}
    s3:
      access-key: ${AWS_S3_ACCESS_KEY:}
      secret-key: ${AWS_S3_SECRET_KEY:}
      region: ${AWS_S3_REGION:ap-northeast-2}
      bucket: ${AWS_S3_BUCKET:}
      presigned-url-expiration: ${AWS_S3_PRESIGNED_URL_EXPIRATION:600}
```

#### 핵심 설계: 환경변수로 스토리지 전환

```
STORAGE_TYPE=local  →  LocalBinaryContentStorage 빈 활성화 (로컬 디스크)
STORAGE_TYPE=s3     →  S3BinaryContentStorage 빈 활성화 (AWS S3)
```

코드를 수정하지 않고 **환경변수만 바꾸면** 저장소를 전환할 수 있습니다.
`${변수명:기본값}` 문법은 Spring의 property placeholder로, 환경변수가 없으면 기본값을 사용합니다.

#### .env 파일 자동 임포트

```yaml
spring:
  config:
    import: optional:file:.env[.properties]
```

프로젝트 루트의 `.env` 파일을 Java Properties 형식으로 읽어옵니다. `optional:` 접두사 덕분에 `.env` 파일이 없어도 오류가 발생하지 않습니다.

---

### 2.3 S3BinaryContentStorage 구현체

> 파일 위치: `src/main/java/com/sprint/mission/discodeit/storage/s3/S3BinaryContentStorage.java`

#### 전체 구조

```
BinaryContentStorage (인터페이스)
├── LocalBinaryContentStorage (type=local 일 때 활성화)
└── S3BinaryContentStorage (type=s3 일 때 활성화) ← 이번에 구현
```

#### 일처리 순서

```
[1] 앱 시작 시 STORAGE_TYPE이 "s3"이면 이 클래스가 Spring Bean으로 등록된다
[2] 생성자에서 application.yaml의 S3 설정값을 @Value로 주입받는다
[3] S3Client와 S3Presigner를 초기화한다
[4] 서비스 계층에서 put/get/download/delete 메서드를 호출한다
```

#### 코드 상세 설명

**클래스 선언부**

```java
@Component  // Spring이 이 클래스를 Bean으로 관리
@ConditionalOnProperty(name = "discodeit.storage.type", havingValue = "s3")
// ↑ discodeit.storage.type이 "s3"일 때만 Bean 등록. "local"이면 이 클래스는 무시됨
public class S3BinaryContentStorage implements BinaryContentStorage {
```

`@ConditionalOnProperty`는 Spring Boot의 조건부 빈 등록 어노테이션입니다.
같은 `BinaryContentStorage` 인터페이스를 구현하는 `LocalBinaryContentStorage`에도 같은 방식(`havingValue = "local"`)이 적용되어 있어서, 동시에 두 개의 빈이 등록되는 것을 방지합니다.

**생성자 (DI + 초기화)**

```java
public S3BinaryContentStorage(
    @Value("${discodeit.storage.s3.access-key}") String accessKey,
    @Value("${discodeit.storage.s3.secret-key}") String secretKey,
    @Value("${discodeit.storage.s3.region}") String region,
    @Value("${discodeit.storage.s3.bucket}") String bucket,
    @Value("${discodeit.storage.s3.presigned-url-expiration:600}") long presignedUrlExpiration
) {
    this.bucket = bucket;
    this.presignedUrlExpiration = presignedUrlExpiration;

    // AWS 자격증명 생성 (Access Key + Secret Key)
    StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
        AwsBasicCredentials.create(accessKey, secretKey)
    );
    Region awsRegion = Region.of(region);  // 예: "ap-northeast-2" → Region.AP_NORTHEAST_2

    // S3 클라이언트 생성 (업로드/다운로드/삭제에 사용)
    this.s3Client = S3Client.builder()
        .region(awsRegion)
        .credentialsProvider(credentialsProvider)
        .build();

    // S3 Presigner 생성 (Presigned URL 생성에 사용)
    this.s3Presigner = S3Presigner.builder()
        .region(awsRegion)
        .credentialsProvider(credentialsProvider)
        .build();
}
```

- `@Value`로 `application.yaml`의 설정값을 주입받습니다
- `StaticCredentialsProvider`: IAM 사용자의 Access Key/Secret Key로 인증합니다
- `S3Client`: 파일 업로드/다운로드/삭제 API를 호출하는 클라이언트
- `S3Presigner`: 임시 접근 URL(Presigned URL)을 생성하는 별도 클라이언트

**put 메서드 (파일 업로드)**

```java
@Override
public UUID put(UUID id, byte[] bytes) {
    PutObjectRequest request = PutObjectRequest.builder()
        .bucket(bucket)        // S3 버킷 이름
        .key(id.toString())    // S3 객체 키 (파일 이름 역할, UUID를 그대로 사용)
        .build();
    s3Client.putObject(request, RequestBody.fromBytes(bytes));  // 바이트 배열을 S3에 업로드
    return id;
}
```

S3에서 "키(key)"는 파일 경로/이름 역할을 합니다. UUID를 키로 사용하면 파일명 충돌이 없습니다.

**get 메서드 (파일 다운로드)**

```java
@Override
public InputStream get(UUID binaryContentId) {
    GetObjectRequest request = GetObjectRequest.builder()
        .bucket(bucket)
        .key(binaryContentId.toString())
        .build();
    return s3Client.getObject(request);  // S3에서 파일을 InputStream으로 반환
}
```

**download 메서드 (Presigned URL 리다이렉트)**

```java
@Override
public ResponseEntity<?> download(BinaryContentDto metaData) {
    String presignedUrl = generatePresignedUrl(
        metaData.id().toString(),     // S3 키
        metaData.contentType()        // 파일 MIME 타입 (예: image/png)
    );
    return ResponseEntity.status(HttpStatus.FOUND)  // 302 Found (리다이렉트)
        .location(URI.create(presignedUrl))          // Location 헤더에 Presigned URL
        .build();
}
```

이 메서드가 이번 요구사항의 핵심입니다.

```
일반 다운로드:  클라이언트 → 서버 → S3 → 서버 → 클라이언트 (서버가 중간 전달)
Presigned URL: 클라이언트 → 서버(URL 생성) → 302 리다이렉트 → 클라이언트가 S3에서 직접 다운로드
```

Presigned URL 방식은 서버가 파일 데이터를 중계하지 않으므로 서버 부하가 줄어듭니다.
HTTP 302 응답의 `Location` 헤더에 Presigned URL을 담으면, 브라우저가 자동으로 S3에서 파일을 다운로드합니다.

**delete 메서드 (파일 삭제)**

```java
@Override
public void delete(UUID id) {
    DeleteObjectRequest request = DeleteObjectRequest.builder()
        .bucket(bucket)
        .key(id.toString())
        .build();
    s3Client.deleteObject(request);
}
```

**generatePresignedUrl 메서드 (비공개 헬퍼)**

```java
private String generatePresignedUrl(String key, String contentType) {
    GetObjectRequest.Builder getObjectBuilder = GetObjectRequest.builder()
        .bucket(bucket)
        .key(key);
    if (contentType != null) {
        getObjectBuilder.responseContentType(contentType);
        // ↑ Presigned URL로 다운로드할 때 Content-Type 헤더를 설정
        //   이렇게 하면 브라우저가 파일 타입을 올바르게 인식합니다
    }

    GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
        .signatureDuration(Duration.ofSeconds(presignedUrlExpiration))
        // ↑ URL 유효기간 (기본 600초 = 10분)
        .getObjectRequest(getObjectBuilder.build())
        .build();

    PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
    return presignedRequest.url().toString();
    // ↑ "https://bucket.s3.amazonaws.com/key?X-Amz-Signature=..." 형태의 URL 반환
}
```

Presigned URL은 일정 시간 동안만 유효한 임시 URL입니다.
이 URL을 가진 사람은 AWS 자격증명 없이도 S3 파일에 접근할 수 있습니다.

---

### 2.4 AWSS3Test (S3 API 직접 테스트)

> 파일 위치: `src/test/java/.../storage/s3/AWSS3Test.java`

#### 설계 의도

S3BinaryContentStorage를 구현하기 전에, AWS S3 SDK가 올바르게 동작하는지 **API 레벨에서 먼저 검증**합니다.
Spring 컨텍스트 없이 독립 실행됩니다.

#### 일처리 순서

```
[1] @BeforeEach에서 .env 파일을 읽어 AWS 자격증명을 로드한다
[2] .env 파일이 없거나 키가 비어있으면 테스트를 건너뛴다 (assumeTrue)
[3] S3Client와 S3Presigner를 직접 생성한다
[4] 각 테스트 메서드에서 업로드/다운로드/PresignedUrl을 검증한다
[5] 테스트 후 업로드한 파일을 삭제한다 (정리)
```

#### 핵심 코드

```java
@BeforeEach
void setUp() throws IOException {
    // .env 파일이 없으면 이 테스트 전체를 건너뛴다 (실패가 아닌 SKIP 처리)
    assumeTrue(Files.exists(Paths.get(".env")),
        ".env 파일이 없으므로 S3 테스트를 건너뜁니다");

    Properties props = new Properties();
    try (FileInputStream fis = new FileInputStream(".env")) {
        props.load(fis);  // .env를 Java Properties 형식으로 읽는다
    }
    // ... S3Client 초기화
}
```

`assumeTrue`는 JUnit의 조건부 실행 기능입니다.
조건이 false이면 테스트를 **실패**가 아닌 **건너뜀(SKIP)**으로 처리합니다.
CI 환경에서 `.env` 파일이 없어도 빌드가 실패하지 않습니다.

**테스트 메서드 3개:**
- `upload()`: S3에 파일 업로드 후 삭제
- `download()`: 업로드 → 다운로드 → 내용 비교 → 삭제
- `generatePresignedUrl()`: 업로드 → URL 생성 → URL에 버킷/키 포함 확인 → 삭제

---

### 2.5 S3BinaryContentStorageTest (구현체 테스트)

> 파일 위치: `src/test/java/.../storage/s3/S3BinaryContentStorageTest.java`

#### 설계 의도

`S3BinaryContentStorage` 클래스의 **메서드 단위**로 동작을 검증합니다.
AWSS3Test과 달리 우리가 만든 구현체를 직접 테스트합니다.

#### 핵심 코드

```java
@BeforeEach
void setUp() throws IOException {
    // ... .env 로드 (AWSS3Test과 동일)

    // 구현체를 직접 생성 (Spring 없이)
    storage = new S3BinaryContentStorage(accessKey, secretKey, region, bucket, expiration);
    testId = UUID.randomUUID();  // 테스트마다 새 UUID
}

@AfterEach
void tearDown() {
    // 테스트 후 S3에서 파일 정리
    if (storage != null && testId != null) {
        try { storage.delete(testId); } catch (Exception ignored) { }
    }
}
```

**테스트 메서드 4개:**

| 메서드 | 검증 내용 |
|--------|----------|
| `put_저장_성공()` | `put()` 호출 후 반환된 UUID가 입력과 같은지 |
| `get_조회_성공()` | `put()` → `get()` → 다운로드한 바이트가 원본과 동일한지 |
| `download_Presigned_URL_리다이렉트()` | 응답이 302 FOUND이고, Location 헤더에 UUID가 포함되어 있는지 |
| `delete_삭제_성공()` | `put()` → `delete()` 호출이 예외 없이 완료되는지 |

---

## 3. AWS를 활용한 배포

### 3.1 application-prod.yaml 운영 설정

> 파일 위치: `src/main/resources/application-prod.yaml`

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}           # ECS에서 환경변수로 RDS 엔드포인트 주입
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
    driver-class-name: org.postgresql.Driver
  jpa:
    show-sql: false                         # 운영 환경에서는 SQL 로그 비활성화
    properties:
      hibernate:
        query.plan_cache_max_size: 64               # 쿼리 캐시 축소 (메모리 절약)
        query.plan_parameter_metadata_max_size: 16  # 파라미터 캐시 축소
  jmx:
    enabled: false    # JMX 비활성화 (모니터링 MBean 메모리 절약)
  main:
    lazy-initialization: true  # 빈 지연 초기화 (기동 시 메모리 피크 감소)

server:
  port: 80            # 컨테이너 내부 포트 (Dockerfile의 EXPOSE 80과 일치)
  tomcat:
    threads:
      max: 20         # 기본 200 → 20 (스레드당 512KB~1MB 절약)
      min-spare: 2    # 기본 10 → 2

logging:
  level:
    com.sprint.mission.discodeit: info  # 운영 환경은 info 레벨
```

#### 왜 모든 DB 설정이 환경변수인가?

```yaml
url: ${SPRING_DATASOURCE_URL}  # 코드에 DB 주소를 하드코딩하지 않는다
```

운영 환경의 DB 주소/비밀번호는 **코드에 포함하면 안 됩니다** (보안상).
ECS에서는 `discodeit.env` 파일을 S3에 저장하고, 태스크 정의에서 이를 로드합니다.

---

### 3.2 ECS 메모리 최적화

t3.micro(1GB RAM)에서 OOM이 발생하여 적용한 최적화입니다.

#### 문제 원인

```
JVM 기본 설정으로 실행하면:
  Heap (자동계산)  + Metaspace (무제한) + 스레드(200×1MB) + CodeCache + ...
  ≈ 250MB          + 200MB+             + 200MB            + 100MB+
  = 750MB 이상 → OS/Docker/ECS Agent 메모리까지 합치면 1GB 초과 → OOM Kill
```

#### 해결: JVM_OPTS

```
JVM_OPTS=-Xmx256m -Xms128m -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=48m
         -XX:MaxDirectMemorySize=16m -XX:+UseSerialGC -XX:-TieredCompilation -Xss512k
```

| 옵션 | 기본값 | 설정값 | 절약량 |
|------|--------|--------|--------|
| `-Xmx` (Heap 최대) | ~250MB | 256MB | - |
| `-XX:MaxMetaspaceSize` | **무제한** | 128MB | ~70MB+ |
| `-XX:ReservedCodeCacheSize` | 240MB | 48MB | ~192MB |
| `-Xss` (스레드 스택) | 1MB×200 | 512KB×20 | ~190MB |
| `-XX:+UseSerialGC` | G1GC (멀티스레드) | 단일스레드 | ~20MB |
| Tomcat 스레드 max | 200 | 20 | (스택 절약에 포함) |

#### 결과

```
최적화 전: ~750MB+ → OOM Kill
최적화 후: ~352MB  → 안정 운영 (900MB 제한의 39%)
```

---

## 4. 요구사항 체크리스트 대조 결과

### 요구사항 1: 애플리케이션 컨테이너화

| 요구사항 | 구현 파일/위치 | 일치 |
|----------|--------------|------|
| Amazon Corretto 17 베이스 이미지 | `Dockerfile` 2행, 8행 | ✅ |
| 작업 디렉토리 `/app` | `Dockerfile` 3행, 9행 | ✅ |
| 불필요한 파일 `.dockerignore`로 제외 | `.dockerignore` | ✅ |
| Gradle Wrapper로 빌드 | `Dockerfile` 5행 | ✅ |
| 80 포트 노출 | `Dockerfile` 17행 | ✅ |
| `PROJECT_NAME=discodeit` 환경변수 | `Dockerfile` 11행 | ✅ |
| `PROJECT_VERSION=1.2-M8` 환경변수 | `Dockerfile` 12행 | ✅ |
| `JVM_OPTS` 환경변수 (기본 빈 문자열) | `Dockerfile` 13행 | ✅ |
| 환경변수로 JAR 파일 이름 추론 | `Dockerfile` 15행 `${PROJECT_NAME}-${PROJECT_VERSION}.jar` | ✅ |
| Docker 이미지 빌드 태그 `local` | `docker build -t discodeit:local .` | ✅ |
| `prod` 프로필로 실행 | `docker-compose.yml` 14행 | ✅ |
| `localhost:8081` 포트 매핑 | `docker run -p 8081:80` 명령으로 가능 | ✅ |
| app + PostgreSQL 서비스 | `docker-compose.yml` app, db 서비스 | ✅ |
| `.env` 파일 활용 (형상관리 제외) | `docker-compose.yml` 12행 + `.gitignore` 52행 | ✅ |
| 로컬 Dockerfile에서 빌드 | `docker-compose.yml` 3행 `build: .` | ✅ |
| 앱 볼륨 (BinaryContent 유지) | `docker-compose.yml` 10행 `app-storage` | ✅ |
| PostgreSQL 볼륨 (데이터 유지) | `docker-compose.yml` 24행 `db-data` | ✅ |
| schema.sql 자동 실행 | `docker-compose.yml` 25행 `docker-entrypoint-initdb.d` | ✅ |
| `depends_on` 의존성 | `docker-compose.yml` 6-8행 `condition: service_healthy` | ✅ |
| Docker Compose `--build` 테스트 | `docker compose up --build` 명령 | ✅ |

### 요구사항 2: BinaryContentStorage 고도화 (AWS S3)

| 요구사항 | 구현 파일/위치 | 일치 |
|----------|--------------|------|
| S3 버킷 생성 (이니셜 포함) | `discodeit-binary-content-storage-ljh-633267` | ✅ |
| 퍼블릭 액세스 차단 | AWS CLI로 설정 완료 | ✅ |
| 버전 관리 비활성화 | AWS CLI로 설정 완료 | ✅ |
| IAM 사용자 `discodeit` 생성 | AWS CLI로 생성 완료 | ✅ |
| `AmazonS3FullAccess` 권한 | AWS CLI로 설정 완료 | ✅ |
| 엑세스 키 → `.env` | `.env` (형상관리 제외) | ✅ |
| `software.amazon.awssdk:s3:2.31.7` | `build.gradle` 34행 | ✅ |
| `AWSS3Test` 클래스 | `src/test/.../storage/s3/AWSS3Test.java` | ✅ |
| Properties로 `.env` 로드 | `AWSS3Test.java` 43-46행 | ✅ |
| 업로드/다운로드/PresignedUrl 테스트 | `AWSS3Test.java` 3개 메서드 | ✅ |
| `@ConditionalOnProperty(havingValue="s3")` | `S3BinaryContentStorage.java` 27행 | ✅ |
| `S3BinaryContentStorageTest` | `src/test/.../storage/s3/S3BinaryContentStorageTest.java` | ✅ |
| `STORAGE_TYPE` 환경변수화 | `application.yaml` 25행 `${STORAGE_TYPE:local}` | ✅ |
| S3 설정 (access-key, secret-key, region, bucket, presigned-url-expiration) | `application.yaml` 28-33행 | ✅ |
| AWS 정보 형상관리 금지 | `.gitignore` 52행 `.env` | ✅ |
| Docker Compose에서도 설정 주입 가능 | `docker-compose.yml` `env_file: .env` | ✅ |
| download PresignedUrl 리다이렉트 | `S3BinaryContentStorage.java` 81-88행 (302 FOUND) | ✅ |

### 요구사항 3: AWS를 활용한 배포

| 요구사항 | 구현 상태 | 일치 |
|----------|----------|------|
| RDS PostgreSQL 17 (db.t4g.micro, 프리티어) | `discodeit-db` 생성 완료 | ✅ |
| 퍼블릭 액세스: 아니오 | `--no-publicly-accessible` | ✅ |
| SSH 터널링으로 DB 초기화 | EC2 경유 psql 실행 완료 | ✅ |
| ECR 퍼블릭 레포지토리 | `public.ecr.aws/q1y9z3p9/discodeit` | ✅ |
| 멀티플랫폼 빌드 (amd64/arm64) | `docker buildx --platform linux/amd64,linux/arm64` | ✅ |
| 태그 latest + 1.2-M8 | ECR 확인 완료 | ✅ |
| ECS 클러스터 `discodeit-cluster` | 생성 완료 (ACTIVE) | ✅ |
| EC2 인스턴스 (t3.micro*) | `i-0e4489704cb3116a9` | ✅ |
| 태스크 정의 `discodeit-task` | revision 4 (bridge, CPU 0.25, 메모리 900MB) | ✅ |
| S3의 discodeit.env 로드 | `environmentFiles` 설정 | ✅ |
| 서비스 `discodeit-service` | 태스크 1개 실행 중 | ✅ |
| HTTP 80 인바운드 규칙 | `sg-010f870b656f5c5e2` | ✅ |
| EC2 퍼블릭 IP 접속 테스트 | `http://3.39.195.223` 정상 응답 | ✅ |

> *요구사항은 t2.micro이나 프리티어 제약으로 t3.micro 사용 (RAM 동일 1GB)

### 검증 결과

| 항목 | 결과 |
|------|------|
| 전체 테스트 (./gradlew clean test) | **BUILD SUCCESSFUL** |
| Docker 이미지 빌드 (docker build) | **성공** |
| ECS 배포 및 접속 테스트 | **정상 (HTTP 200)** |
| **요구사항 일치율** | **100% (모든 항목 충족)** |
