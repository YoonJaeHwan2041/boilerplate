package com.jelly.boilerplate.global.s3;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yaml 의 {@code aws.s3.*} 바인딩.
 *
 * <pre>
 * aws:
 *   s3:
 *     region: ap-northeast-2
 *     bucket: my-bucket
 *     access-key:        # 비우면 DefaultCredentialsProvider(환경변수 / ~/.aws / IAM Role) 사용
 *     secret-key:
 *     endpoint:          # LocalStack / MinIO 로컬 테스트용 (예: http://localhost:4566). 비우면 실제 AWS
 *     path-style-access: false
 * </pre>
 */
@ConfigurationProperties(prefix = "aws.s3")
public record S3Properties(
    String region,
    String bucket,
    String accessKey,
    String secretKey,
    String endpoint,
    boolean pathStyleAccess
) {
    public boolean hasStaticCredentials() {
        return accessKey != null && !accessKey.isBlank()
            && secretKey != null && !secretKey.isBlank();
    }

    public boolean hasCustomEndpoint() {
        return endpoint != null && !endpoint.isBlank();
    }
}
