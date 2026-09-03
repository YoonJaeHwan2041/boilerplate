package com.jelly.boilerplate.global.s3;

import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

/**
 * S3 클라이언트 빈 구성.
 *
 * <ul>
 *   <li>{@code access-key}/{@code secret-key} 가 있으면 정적 자격증명, 없으면 기본 체인
 *       (환경변수 → ~/.aws → EC2/ECS IAM Role)</li>
 *   <li>{@code endpoint} 가 설정되면 LocalStack/MinIO 로 간주하고 path-style 접근 강제</li>
 * </ul>
 *
 * <p>빈 생성 시점에는 실제 연결/인증을 하지 않으므로(지연 평가) 자격증명이 없어도 컨텍스트는 뜬다.
 */
@Configuration
@EnableConfigurationProperties(S3Properties.class)
@RequiredArgsConstructor
public class S3Config {

    private final S3Properties properties;

    @Bean
    public S3Client s3Client() {
        boolean pathStyle = properties.pathStyleAccess() || properties.hasCustomEndpoint();

        var builder = S3Client.builder()
            .region(Region.of(properties.region()))
            .credentialsProvider(credentialsProvider())
            .serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(pathStyle)
                .build());

        if (properties.hasCustomEndpoint()) {
            builder.endpointOverride(URI.create(properties.endpoint()));
        }
        return builder.build();
    }

    private AwsCredentialsProvider credentialsProvider() {
        if (properties.hasStaticCredentials()) {
            return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.accessKey(), properties.secretKey()));
        }
        return DefaultCredentialsProvider.create();
    }
}
