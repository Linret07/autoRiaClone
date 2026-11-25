package com.autoriaclone.service.storage;

import com.autoriaclone.config.FeatureFlagsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.features.storage.provider", havingValue = "s3")
public class S3FileStorageService implements FileStorageService {

    private final FeatureFlagsProperties features;

    @Override
    public String save(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("Uploaded file is empty");
        }
        String bucket = features.getStorage().getS3Bucket();
        String region = features.getStorage().getAwsRegion();
        String key = "ads/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

        log.info("[AWS S3] Stored file '{}' ({} bytes) into bucket '{}' (region: {})",
                key, file.getSize(), bucket, region);

        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
    }
}
