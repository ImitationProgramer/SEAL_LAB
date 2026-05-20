package com.seal.seal_lab.infra.storage;

import com.seal.seal_lab.infra.config.S3StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3StorageService {

    private final S3Client s3Client;
    private final S3StorageProperties properties;

    public String uploadProfileImage(MultipartFile file) throws IOException {
        return upload(file, properties.getProfilePrefix());
    }

    public String uploadGalleryImage(MultipartFile file) throws IOException {
        return upload(file, properties.getGalleryPrefix());
    }

    public void deleteByUrl(String imageUrl) {
        String key = extractKey(imageUrl);
        if (key == null) {
            return;
        }

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .build());
            log.info("[S3-DELETE] Deleted object. bucket={} key={}", properties.getBucket(), key);
        } catch (S3Exception ex) {
            log.warn("[S3-DELETE] Failed to delete object. bucket={} key={} message={}",
                    properties.getBucket(), key, ex.getMessage());
        }
    }

    private String upload(MultipartFile file, String prefix) throws IOException {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null
                ? "upload.bin"
                : file.getOriginalFilename());
        String key = prefix + "/" + UUID.randomUUID() + "_" + originalFilename;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        String publicUrl = resolvePublicUrl(key);
        log.info("[S3-UPLOAD] Uploaded object. bucket={} key={} url={}", properties.getBucket(), key, publicUrl);
        return publicUrl;
    }

    private String resolvePublicUrl(String key) {
        S3Utilities s3Utilities = s3Client.utilities();
        return s3Utilities.getUrl(GetUrlRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .build()).toExternalForm();
    }

    private String extractKey(String imageUrl) {
        if (!StringUtils.hasText(imageUrl) || imageUrl.startsWith("/")) {
            return null;
        }

        try {
            URI uri = URI.create(imageUrl);
            String path = uri.getPath();
            if (!StringUtils.hasText(path) || "/".equals(path)) {
                return null;
            }
            return path.startsWith("/") ? path.substring(1) : path;
        } catch (IllegalArgumentException ex) {
            log.warn("[S3-DELETE] Invalid image URL. url={}", imageUrl);
            return null;
        }
    }
}
