# ============================================================
# 멀티 스테이지 빌드: 빌드 환경과 런타임 환경을 분리하여 최종 이미지 크기를 최소화
# ============================================================

# Stage 1: 빌드 (Gradle로 JAR 생성)
FROM amazoncorretto:17 AS build
WORKDIR /app

# 의존성 파일만 먼저 복사하여 레이어 캐시 최적화
# → 소스 코드가 변경되어도 의존성 다운로드를 건너뜀
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# 소스 코드 복사 후 테스트 제외하고 빌드 (-x test)
COPY src ./src
RUN ./gradlew clean build -x test --no-daemon

# Stage 2: 런타임 (Alpine 경량 이미지로 실행만 담당)
FROM amazoncorretto:17-alpine
WORKDIR /app

# 프로젝트 정보 환경변수 (JAR 파일명 추론에 활용)
ENV PROJECT_NAME=discodeit
ENV PROJECT_VERSION=1.2-M8
# JVM 옵션 (ECS 배포 시 메모리 제한 설정에 활용)
ENV JVM_OPTS=""

# 빌드 스테이지에서 생성된 JAR 파일만 복사
COPY --from=build /app/build/libs/${PROJECT_NAME}-${PROJECT_VERSION}.jar app.jar

EXPOSE 80

ENTRYPOINT ["sh", "-c", "java $JVM_OPTS -jar app.jar"]
