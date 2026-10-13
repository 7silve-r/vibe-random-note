package com.silver.storage.validation;

import static org.junit.jupiter.api.Assertions.*;

import com.silver.storage.exception.FileStorageException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class UploadValidatorTest {
    @Test
    void audioLimit() {
        var file = org.mockito.Mockito.mock(org.springframework.web.multipart.MultipartFile.class);
        org.mockito.Mockito.when(file.getSize()).thenReturn(50L * 1024 * 1024 + 1);
        assertEquals(
                413,
                assertThrows(FileStorageException.class, () -> UploadValidator.validate(file, true))
                        .getCode());
    }

    @Test
    void fakeImage() {
        var error =
                assertThrows(
                        FileStorageException.class,
                        () ->
                                UploadValidator.validate(
                                        new MockMultipartFile(
                                                "file",
                                                "image.png",
                                                "image/png",
                                                "<script>bad</script>".getBytes()),
                                        false));
        assertEquals(415, error.getCode());
    }

    @Test
    void realPng() throws Exception {
        var bytes = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2, 2, 1), "png", bytes);
        var result =
                UploadValidator.validate(
                        new MockMultipartFile(
                                "file", "../../script.exe", "text/plain", bytes.toByteArray()),
                        false);
        assertEquals(".png", result.extension());
        assertEquals("image/png", result.type());
    }

    @Test
    void size() {
        assertEquals(
                400,
                assertThrows(
                                FileStorageException.class,
                                () ->
                                        UploadValidator.validate(
                                                new MockMultipartFile("file", new byte[0]), false))
                        .getCode());
        assertEquals(
                413,
                assertThrows(
                                FileStorageException.class,
                                () ->
                                        UploadValidator.validate(
                                                new MockMultipartFile(
                                                        "file", new byte[5 * 1024 * 1024 + 1]),
                                                false))
                        .getCode());
    }

    @Test
    void dimensions() throws Exception {
        var bytes = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(4097, 1, 1), "png", bytes);
        assertEquals(
                400,
                assertThrows(
                                FileStorageException.class,
                                () ->
                                        UploadValidator.validate(
                                                new MockMultipartFile("file", bytes.toByteArray()),
                                                false))
                        .getCode());
    }

    @Test
    void fakeAudio() {
        assertEquals(
                415,
                assertThrows(
                                FileStorageException.class,
                                () ->
                                        UploadValidator.validate(
                                                new MockMultipartFile(
                                                        "file",
                                                        "song.mp3",
                                                        "audio/mpeg",
                                                        "not audio".getBytes()),
                                                true))
                        .getCode());
    }
}
