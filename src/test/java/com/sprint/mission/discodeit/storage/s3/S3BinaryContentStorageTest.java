package com.sprint.mission.discodeit.storage.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.sprint.mission.discodeit.dto.data.BinaryContentDto;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * S3BinaryContentStorage 통합 테스트.
 * AWS 자격증명은 .env 파일(로컬) 또는 환경변수(CI)에서 로드된다.
 */
class S3BinaryContentStorageTest {

  private S3BinaryContentStorage storage;
  private UUID testId;

  @BeforeEach
  void setUp() throws IOException {
    Properties props = loadConfig();

    String accessKey = props.getProperty("AWS_S3_ACCESS_KEY", "").trim();
    String secretKey = props.getProperty("AWS_S3_SECRET_KEY", "").trim();
    String region = props.getProperty("AWS_S3_REGION", "ap-northeast-2").trim();
    String bucket = props.getProperty("AWS_S3_BUCKET", "").trim();
    long expiration = Long.parseLong(
        props.getProperty("AWS_S3_PRESIGNED_URL_EXPIRATION", "600").trim());

    assumeTrue(!accessKey.isEmpty() && !secretKey.isEmpty() && !bucket.isEmpty(),
        "AWS S3 자격증명이 설정되지 않아 테스트를 건너뜁니다 (.env 또는 환경변수)");

    storage = new S3BinaryContentStorage(accessKey, secretKey, region, bucket, expiration);
    testId = UUID.randomUUID();
  }

  @AfterEach
  void tearDown() {
    if (storage != null && testId != null) {
      try {
        storage.delete(testId);
      } catch (Exception ignored) {
      }
    }
  }

  @Test
  void put_저장_성공() {
    byte[] content = "S3 Storage 테스트".getBytes(StandardCharsets.UTF_8);

    UUID result = storage.put(testId, content);

    assertThat(result).isEqualTo(testId);
  }

  @Test
  void get_조회_성공() throws IOException {
    byte[] content = "S3 Storage get 테스트".getBytes(StandardCharsets.UTF_8);
    storage.put(testId, content);

    try (InputStream is = storage.get(testId)) {
      byte[] downloaded = is.readAllBytes();
      assertThat(downloaded).isEqualTo(content);
    }
  }

  @Test
  void download_Presigned_URL_리다이렉트() {
    byte[] content = "S3 download 테스트".getBytes(StandardCharsets.UTF_8);
    storage.put(testId, content);

    BinaryContentDto metaData = new BinaryContentDto(
        testId, "test.txt", (long) content.length, "text/plain"
    );

    ResponseEntity<?> response = storage.download(metaData);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
    assertThat(response.getHeaders().getLocation()).isNotNull();
    assertThat(response.getHeaders().getLocation().toString()).contains(testId.toString());
  }

  @Test
  void delete_삭제_성공() {
    byte[] content = "S3 delete 테스트".getBytes(StandardCharsets.UTF_8);
    storage.put(testId, content);

    storage.delete(testId);

    // 삭제 후 testId를 null로 설정하여 tearDown에서 중복 삭제 방지
    testId = null;
  }

  /**
   * 설정 로드: .env 파일이 있으면 파일에서, 없으면 환경변수에서 로드한다.
   * 로컬 개발 환경은 .env 파일, CI 환경은 GitHub Secrets 주입 환경변수를 사용한다.
   */
  private Properties loadConfig() throws IOException {
    Properties props = new Properties();
    if (Files.exists(Paths.get(".env"))) {
      try (FileInputStream fis = new FileInputStream(".env")) {
        props.load(fis);
      }
    } else {
      for (String key : new String[]{
          "AWS_S3_ACCESS_KEY", "AWS_S3_SECRET_KEY", "AWS_S3_REGION",
          "AWS_S3_BUCKET", "AWS_S3_PRESIGNED_URL_EXPIRATION"}) {
        String value = System.getenv(key);
        if (value != null) {
          props.setProperty(key, value);
        }
      }
    }
    return props;
  }
}
