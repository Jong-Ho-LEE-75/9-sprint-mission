package com.sprint.mission.discodeit.storage.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

/**
 * AWS S3 SDK 직접 테스트.
 * AWS 자격증명은 .env 파일(로컬) 또는 환경변수(CI)에서 로드된다.
 */
class AWSS3Test {

  private S3Client s3Client;
  private S3Presigner s3Presigner;
  private String bucket;

  @BeforeEach
  void setUp() throws IOException {
    Properties props = loadConfig();

    String accessKey = props.getProperty("AWS_S3_ACCESS_KEY", "").trim();
    String secretKey = props.getProperty("AWS_S3_SECRET_KEY", "").trim();
    String region = props.getProperty("AWS_S3_REGION", "ap-northeast-2").trim();
    bucket = props.getProperty("AWS_S3_BUCKET", "").trim();

    assumeTrue(!accessKey.isEmpty() && !secretKey.isEmpty() && !bucket.isEmpty(),
        "AWS S3 자격증명이 설정되지 않아 테스트를 건너뜁니다 (.env 또는 환경변수)");

    StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
        AwsBasicCredentials.create(accessKey, secretKey)
    );
    Region awsRegion = Region.of(region);

    s3Client = S3Client.builder()
        .region(awsRegion)
        .credentialsProvider(credentialsProvider)
        .build();

    s3Presigner = S3Presigner.builder()
        .region(awsRegion)
        .credentialsProvider(credentialsProvider)
        .build();
  }

  @Test
  void upload() {
    String key = "test/" + UUID.randomUUID();
    byte[] content = "Hello S3!".getBytes(StandardCharsets.UTF_8);

    PutObjectRequest request = PutObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .contentType("text/plain")
        .build();
    s3Client.putObject(request, RequestBody.fromBytes(content));

    // 정리
    s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
  }

  @Test
  void download() throws IOException {
    String key = "test/" + UUID.randomUUID();
    byte[] content = "Hello S3 Download!".getBytes(StandardCharsets.UTF_8);

    // 업로드
    s3Client.putObject(
        PutObjectRequest.builder().bucket(bucket).key(key).build(),
        RequestBody.fromBytes(content)
    );

    // 다운로드
    try (InputStream is = s3Client.getObject(
        GetObjectRequest.builder().bucket(bucket).key(key).build())) {
      byte[] downloaded = is.readAllBytes();
      assertThat(downloaded).isEqualTo(content);
    }

    // 정리
    s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
  }

  @Test
  void generatePresignedUrl() {
    String key = "test/" + UUID.randomUUID();
    byte[] content = "Presigned URL test".getBytes(StandardCharsets.UTF_8);

    // 업로드
    s3Client.putObject(
        PutObjectRequest.builder().bucket(bucket).key(key).build(),
        RequestBody.fromBytes(content)
    );

    // Presigned URL 생성
    GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
        .signatureDuration(Duration.ofSeconds(600))
        .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
        .build();
    PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
    String url = presignedRequest.url().toString();

    assertThat(url).contains(bucket);
    assertThat(url).contains(key);

    // 정리
    s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
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
