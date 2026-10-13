package com.silver.storage.service;

import com.silver.storage.model.UploadTarget;
import java.io.InputStream;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {
    String upload(MultipartFile file, UploadTarget target);

    InputStream read(String url);

    void delete(String url);

    default void deleteQuietly(String url) {
        if (url == null || url.isBlank()) return;
        try {
            delete(url);
        } catch (RuntimeException ex) {
            LoggerFactory.getLogger(FileStorage.class).warn("文件清理失败，需后续清理: {}", url, ex);
        }
    }
}
