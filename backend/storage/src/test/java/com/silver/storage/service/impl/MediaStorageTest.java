package com.silver.storage.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.silver.storage.model.UploadTarget;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

class MediaStorageTest {
    private MinioClient client;
    private MinioFileStorage service;

    @BeforeEach
    void setUp() {
        client = mock(MinioClient.class);
        service =
                new MinioFileStorage(
                        client, "private", "test-music", "/uploads/", "http://storage.invalid");
    }

    @Test
    void empty() {
        assertThrows(
                com.silver.storage.exception.FileStorageException.class,
                () ->
                        service.upload(
                                new MockMultipartFile("file", new byte[0]),
                                UploadTarget.SONG_AUDIO));
        verifyNoInteractions(client);
    }

    @Test
    void upload() throws Exception {
        MockMultipartFile file = new MockMultipartFile("songs", "tone.wav", "audio/wav", wav());
        String url = service.upload(file, UploadTarget.SONG_AUDIO);
        ArgumentCaptor<PutObjectArgs> request = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(client).putObject(request.capture());
        assertEquals("test-music", request.getValue().bucket());
        assertTrue(request.getValue().object().startsWith("songs/"));
        assertTrue(request.getValue().object().endsWith(".wav"));
        assertEquals("http://storage.invalid/test-music/" + request.getValue().object(), url);
    }

    @Test
    void offline() throws Exception {
        when(client.putObject(any(PutObjectArgs.class)))
                .thenThrow(new IllegalStateException("storage-offline"));
        MockMultipartFile file = new MockMultipartFile("songs", "tone.wav", "audio/wav", wav());
        RuntimeException error =
                assertThrows(
                        RuntimeException.class,
                        () -> service.upload(file, UploadTarget.SONG_AUDIO));
        assertEquals("文件上传失败，请稍后重试", error.getMessage());
        assertEquals("storage-offline", error.getCause().getMessage());
    }

    private byte[] wav() {
        byte[] bytes = new byte[44];
        System.arraycopy(
                "RIFF".getBytes(java.nio.charset.StandardCharsets.US_ASCII), 0, bytes, 0, 4);
        System.arraycopy(
                "WAVE".getBytes(java.nio.charset.StandardCharsets.US_ASCII), 0, bytes, 8, 4);
        return bytes;
    }
}
