package com.jelly.boilerplate.global.s3;

import com.jelly.boilerplate.global.exception.BusinessException;
import com.jelly.boilerplate.global.response.code.FileExceptionCode;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * S3 업로드 / 삭제.
 *
 * <p>저장 키 형식: {@code <dir>/yyyy/MM/dd/<uuid><.ext>} — 원본 파일명은 키에 쓰지 않는다
 * (한글/공백/중복/경로조작 방지). 원본 파일명이 필요하면 DB 컬럼에 별도로 저장할 것.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3Uploader {

    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final S3Client s3Client;
    private final S3Properties properties;

    /**
     * @param file 업로드할 파일
     * @param dir  버킷 내 상위 폴더 (예: "profile", "board"). null/blank 이면 루트
     * @return 저장된 key 와 접근 URL
     */
    public UploadResult upload(MultipartFile file, String dir) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(FileExceptionCode.EMPTY_FILE);
        }

        String key = buildKey(dir, file.getOriginalFilename());
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException | S3Exception e) {
            log.error("[{}] S3 업로드 실패 key={}", FileExceptionCode.UPLOAD_FAILED.getCode(), key, e);
            throw new BusinessException(FileExceptionCode.UPLOAD_FAILED);
        }
        return new UploadResult(key, toUrl(key));
    }

    public void delete(String key) {
        try {
            s3Client.deleteObject(b -> b.bucket(properties.bucket()).key(key));
        } catch (S3Exception e) {
            log.error("[{}] S3 삭제 실패 key={}", FileExceptionCode.DELETE_FAILED.getCode(), key, e);
            throw new BusinessException(FileExceptionCode.DELETE_FAILED);
        }
    }

    // ------------------------------------------------------------
    private String buildKey(String dir, String originalFilename) {
        String prefix = (dir == null || dir.isBlank())
            ? ""
            : dir.replaceAll("^/+|/+$", "") + "/";
        String random = UUID.randomUUID().toString().replace("-", "");
        return prefix + LocalDate.now().format(DATE_PATH) + "/" + random + extensionOf(originalFilename);
    }

    private String extensionOf(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return (dot == -1) ? "" : filename.substring(dot).toLowerCase();
    }

    private String toUrl(String key) {
        if (properties.hasCustomEndpoint()) {
            // LocalStack/MinIO: path-style
            return properties.endpoint().replaceAll("/+$", "") + "/" + properties.bucket() + "/" + key;
        }
        return "https://" + properties.bucket() + ".s3." + properties.region() + ".amazonaws.com/" + key;
    }

    public record UploadResult(String key, String url) {}
}
