package com.jelly.boilerplate.file;

import com.jelly.boilerplate.global.response.ApiResponse;
import com.jelly.boilerplate.global.s3.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * S3 파일 업로드 데모.
 *
 * <p>{@code /api/v1/files/**} 는 permitAll 이 아니므로 <b>유효한 JWT 가 필요</b>하다(업로드는 인증 사용자만).
 * 실제 프로젝트에서는 반환된 {@code key} 를 도메인 엔티티(프로필 이미지, 첨부파일 등)에 저장해 두고,
 * 화면에 보여줄 때 {@code url}(공개 버킷 기준)을 사용한다.
 */
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final S3Uploader s3Uploader;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<S3Uploader.UploadResult> upload(
        @RequestPart("file") MultipartFile file,
        @RequestParam(value = "dir", defaultValue = "uploads") String dir
    ) {
        return ApiResponse.success(s3Uploader.upload(file, dir));
    }

    @DeleteMapping
    public ApiResponse<Void> delete(@RequestParam String key) {
        s3Uploader.delete(key);
        return ApiResponse.noContentSuccess();
    }
}
