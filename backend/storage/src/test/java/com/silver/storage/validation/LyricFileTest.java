package com.silver.storage.validation;

import static org.junit.jupiter.api.Assertions.*;

import com.silver.storage.exception.FileStorageException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class LyricFileTest {
    @Test
    void normalizeTextAndRejectInvalidContent() {
        assertEquals(
                "歌词\n第二行",
                LyricFile.read(
                        new MockMultipartFile(
                                "file",
                                "SONG.LRC",
                                "text/plain",
                                "\uFEFF歌词\r\n第二行".getBytes(StandardCharsets.UTF_8))));
        for (byte[] bytes :
                new byte[][] {
                    new byte[0], new byte[] {(byte) 0xff}, new byte[] {0, 1, 2}, new byte[60001]
                })
            assertThrows(
                    FileStorageException.class,
                    () ->
                            LyricFile.read(
                                    new MockMultipartFile("file", "a.txt", "text/plain", bytes)));
    }
}
