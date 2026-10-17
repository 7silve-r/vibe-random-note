package com.silver.storage.service.impl;

import com.silver.storage.exception.FileStorageException;
import com.silver.storage.model.UploadTarget;
import com.silver.storage.service.FileStorage;
import com.silver.storage.validation.LyricFile;
import com.silver.storage.validation.UploadValidator;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MinioFileStorage implements FileStorage {
    private final MinioClient client;
    private final String privateBucket;
    private final String publicBucket;
    private final String privatePrefix;
    private final String publicPrefix;

    public MinioFileStorage(
            MinioClient client,
            @Value("${minio.private-bucket}") String privateBucket,
            @Value("${minio.bucket}") String publicBucket,
            @Value("${file.access-url-prefix}") String privatePrefix,
            @Value("${minio.endpoint}") String endpoint) {
        if (privateBucket.equals(publicBucket)) throw new IllegalArgumentException("日记私有桶不能与音乐桶相同");
        if (!privatePrefix.matches("/(?:[A-Za-z0-9_-]+/)+"))
            throw new IllegalArgumentException("文件访问前缀格式错误");
        this.client = client;
        this.privateBucket = privateBucket;
        this.publicBucket = publicBucket;
        this.privatePrefix = privatePrefix;
        this.publicPrefix = endpoint.replaceAll("/+$", "") + "/" + publicBucket + "/";
    }

    @Override
    public String upload(MultipartFile file, UploadTarget target) {
        if (target == null) throw new IllegalArgumentException("请选择上传用途");
        try {
            var content =
                    target == UploadTarget.SONG_LYRIC
                            ? new UploadValidator.Content(
                                    LyricFile.read(file).getBytes(StandardCharsets.UTF_8),
                                    "text/plain; charset=utf-8",
                                    ".lrc")
                            : UploadValidator.validate(file, target.audio());
            String key = target.folder() + "/" + UUID.randomUUID() + content.extension();
            if (target.privateBucket()) requirePrivateBucket();
            try (var stream = new ByteArrayInputStream(content.bytes())) {
                client.putObject(
                        PutObjectArgs.builder().bucket(bucket(target)).object(key).stream(
                                        stream, content.bytes().length, -1)
                                .contentType(content.type())
                                .build());
            }
            return (target.privateBucket() ? privatePrefix : publicPrefix) + key;
        } catch (FileStorageException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new FileStorageException("文件上传失败，请稍后重试", ex);
        }
    }

    @Override
    public InputStream read(String url) {
        var object = resolve(url);
        if (object == null) throw new FileStorageException(404, "文件不存在");
        try {
            if (object.target().privateBucket()) requirePrivateBucket();
            return client.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket(object.target()))
                            .object(object.key())
                            .build());
        } catch (ErrorResponseException ex) {
            if ("NoSuchKey".equals(ex.errorResponse().code()))
                throw new FileStorageException(404, "文件不存在");
            throw new FileStorageException("文件读取失败，请稍后重试", ex);
        } catch (Exception ex) {
            throw new FileStorageException("文件读取失败，请稍后重试", ex);
        }
    }

    @Override
    public void delete(String url) {
        var object = resolve(url);
        if (object == null) return;
        try {
            client.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket(object.target()))
                            .object(object.key())
                            .build());
        } catch (Exception ex) {
            throw new FileStorageException("文件删除失败", ex);
        }
    }

    private StoredObject resolve(String url) {
        if (url == null) return null;
        for (var target : UploadTarget.values()) {
            String prefix = (target.privateBucket() ? privatePrefix : publicPrefix);
            if (!url.startsWith(prefix)) continue;
            String key = url.substring(prefix.length());
            String extensions =
                    target == UploadTarget.SONG_LYRIC
                            ? "lrc|txt"
                            : target.audio() ? "mp3|wav|ogg|flac|m4a" : "png|jpg|jpeg|webp";
            if (key.matches(target.folder() + "/[0-9a-fA-F-]{36}\\.(?:" + extensions + ")"))
                return new StoredObject(target, key);
        }
        return null;
    }

    private String bucket(UploadTarget target) {
        return target.privateBucket() ? privateBucket : publicBucket;
    }

    private void requirePrivateBucket() throws Exception {
        String policy =
                client.getBucketPolicy(GetBucketPolicyArgs.builder().bucket(privateBucket).build());
        if (policy != null && !policy.isBlank()) throw new IllegalStateException("日记私有桶不能配置匿名访问策略");
    }

    private record StoredObject(UploadTarget target, String key) {}
}
