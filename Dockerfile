# Stage 1: 빌드
FROM amazoncorretto:17 AS build
WORKDIR /app

# 레이어 캐시 최적화: 의존성 파일 먼저 복사
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# 소스 코드 복사 후 빌드
COPY src ./src
RUN ./gradlew clean build -x test --no-daemon

# Stage 2: 런타임 (경량 이미지)
FROM amazoncorretto:17-alpine
WORKDIR /app

ENV PROJECT_NAME=discodeit
ENV PROJECT_VERSION=1.2-M8
ENV JVM_OPTS=""

COPY --from=build /app/build/libs/${PROJECT_NAME}-${PROJECT_VERSION}.jar app.jar

EXPOSE 80

ENTRYPOINT ["sh", "-c", "java $JVM_OPTS -jar app.jar"]
