package com.silver.storage.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.silver.storage.exception.FileStorageException;
import com.silver.storage.model.UploadTarget;
import com.silver.storage.support.TestFiles;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MinioFileStorageTest {
    MinioClient client = mock(MinioClient.class);
    MinioFileStorage storage =
            new MinioFileStorage(client, "private", "music", "/uploads/", "http://storage.invalid");

    @Test
    void savePng() throws Exception {
        String url = storage.upload(TestFiles.png(), UploadTarget.ARTICLE_COVER);
        assertTrue(url.matches("/uploads/covers/[0-9a-f-]{36}\\.png"));
        var args = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(client).putObject(args.capture());
        assertEquals("private", args.getValue().bucket());
        assertEquals(url.substring(9), args.getValue().object());
        assertEquals("image/png", args.getValue().contentType());
    }

    @Test
    void badFolder() {
        assertThrows(IllegalArgumentException.class, () -> storage.upload(TestFiles.png(), null));
        verifyNoInteractions(client);
    }

    @Test
    void distinctBuckets() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new MinioFileStorage(
                                client, "music", "music", "/uploads/", "http://storage.invalid"));
    }

    @Test
    void deleteOwn() throws Exception {
        String key = "avatars/12345678-1234-1234-1234-123456789012.png";
        storage.deleteQuietly("/uploads/" + key);
        var args = ArgumentCaptor.forClass(RemoveObjectArgs.class);
        verify(client).removeObject(args.capture());
        assertEquals("private", args.getValue().bucket());
        assertEquals(key, args.getValue().object());
    }

    @Test
    void keepForeign() {
        storage.deleteQuietly("/uploads/covers/../keep.png");
        storage.deleteQuietly("https://other.invalid/keep.png");
        storage.deleteQuietly(
                "http://storage.invalid/music/covers/12345678-1234-1234-1234-123456789012.png");
        storage.deleteQuietly("/uploads/songs/12345678-1234-1234-1234-123456789012.mp3");
        storage.deleteQuietly(
                "http://storage.invalid/music/songs/12345678-1234-1234-1234-123456789012.mp3?x=1");
        verifyNoInteractions(client);
    }

    @Test
    void deleteMusic() throws Exception {
        String key = "songs/12345678-1234-1234-1234-123456789012.mp3";
        storage.delete("http://storage.invalid/music/" + key);
        var args = ArgumentCaptor.forClass(RemoveObjectArgs.class);
        verify(client).removeObject(args.capture());
        assertEquals("music", args.getValue().bucket());
        assertEquals(key, args.getValue().object());
    }

    @Test
    void readPrivate() throws Exception {
        String key = "covers/12345678-1234-1234-1234-123456789012.png";
        var response = mock(GetObjectResponse.class);
        when(client.getObject(any(GetObjectArgs.class))).thenReturn(response);
        assertSame(response, storage.read("/uploads/" + key));
        var args = ArgumentCaptor.forClass(GetObjectArgs.class);
        verify(client).getObject(args.capture());
        assertEquals("private", args.getValue().bucket());
        assertEquals(key, args.getValue().object());
    }

    @Test
    void missingAndUnavailable() throws Exception {
        var error = mock(ErrorResponse.class);
        when(error.code()).thenReturn("NoSuchKey");
        var missing = new ErrorResponseException(error, null, "");
        when(client.getObject(any(GetObjectArgs.class))).thenThrow(missing);
        String key = "covers/12345678-1234-1234-1234-123456789012.png";
        assertEquals(
                404,
                assertThrows(FileStorageException.class, () -> storage.read("/uploads/" + key))
                        .getCode());
        when(error.code()).thenReturn("AccessDenied");
        assertThrows(FileStorageException.class, () -> storage.read("/uploads/" + key));
    }

    @Test
    void rejectPublicPolicy() throws Exception {
        when(client.getBucketPolicy(any(GetBucketPolicyArgs.class)))
                .thenReturn("{\"Statement\":[{\"Principal\":\"*\"}]}");
        assertThrows(
                FileStorageException.class,
                () -> storage.upload(TestFiles.png(), UploadTarget.ARTICLE_COVER));
        assertThrows(
                FileStorageException.class,
                () -> storage.read("/uploads/covers/12345678-1234-1234-1234-123456789012.png"));
        verify(client, never()).putObject(any(PutObjectArgs.class));
        verify(client, never()).getObject(any(GetObjectArgs.class));
    }
}
