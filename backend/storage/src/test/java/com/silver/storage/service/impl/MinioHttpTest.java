package com.silver.storage.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.silver.storage.exception.FileStorageException;
import com.silver.storage.model.UploadTarget;
import com.silver.storage.support.TestFiles;
import com.sun.net.httpserver.HttpServer;
import io.minio.MinioClient;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class MinioHttpTest {
    @Test
    void sdkRoundTrip() throws Exception {
        Map<String, byte[]> objects = new ConcurrentHashMap<>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/",
                exchange -> {
                    try (exchange) {
                        String key = exchange.getRequestURI().getPath();
                        String method = exchange.getRequestMethod();
                        if ("policy=".equals(exchange.getRequestURI().getQuery())
                                || !objects.containsKey(key) && method.equals("GET")) {
                            String code =
                                    "policy=".equals(exchange.getRequestURI().getQuery())
                                            ? "NoSuchBucketPolicy"
                                            : "NoSuchKey";
                            byte[] body =
                                    ("<Error><Code>"
                                                    + code
                                                    + "</Code><Message>missing</Message><Resource>"
                                                    + key
                                                    + "</Resource><RequestId>test</RequestId></Error>")
                                            .getBytes(StandardCharsets.UTF_8);
                            exchange.getResponseHeaders().set("Content-Type", "application/xml");
                            exchange.sendResponseHeaders(404, body.length);
                            exchange.getResponseBody().write(body);
                        } else if (method.equals("PUT")) {
                            objects.put(key, exchange.getRequestBody().readAllBytes());
                            exchange.getResponseHeaders().set("ETag", "\"test-etag\"");
                            exchange.sendResponseHeaders(200, -1);
                        } else if (method.equals("GET")) {
                            byte[] body = objects.get(key);
                            exchange.sendResponseHeaders(200, body.length);
                            exchange.getResponseBody().write(body);
                        } else if (method.equals("DELETE")) {
                            objects.remove(key);
                            exchange.sendResponseHeaders(204, -1);
                        } else {
                            exchange.sendResponseHeaders(405, -1);
                        }
                    }
                });
        server.start();
        try {
            String endpoint = "http://127.0.0.1:" + server.getAddress().getPort();
            var client =
                    MinioClient.builder()
                            .endpoint(endpoint)
                            .region("us-east-1")
                            .credentials("test-access", "test-secret-key")
                            .build();
            var storage = new MinioFileStorage(client, "private", "music", "/uploads/", endpoint);
            for (var target : UploadTarget.values()) {
                byte[] audio = new byte[44];
                System.arraycopy("RIFF".getBytes(StandardCharsets.US_ASCII), 0, audio, 0, 4);
                System.arraycopy("WAVE".getBytes(StandardCharsets.US_ASCII), 0, audio, 8, 4);
                var file =
                        target == UploadTarget.SONG_LYRIC
                                ? new MockMultipartFile(
                                        "file",
                                        "song.lrc",
                                        "text/plain",
                                        "[00:01.00]歌词".getBytes(StandardCharsets.UTF_8))
                                : target.audio()
                                        ? new MockMultipartFile("file", audio)
                                        : TestFiles.png();
                String url = storage.upload(file, target);
                String bucket = target.privateBucket() ? "private" : "music";
                assertTrue(
                        objects.keySet().stream()
                                .anyMatch(
                                        key ->
                                                key.startsWith(
                                                        "/"
                                                                + bucket
                                                                + "/"
                                                                + target.folder()
                                                                + "/")));
                try (var stream = storage.read(url)) {
                    assertArrayEquals(objects.values().iterator().next(), stream.readAllBytes());
                }
                storage.delete(url);
                assertTrue(objects.isEmpty());
                assertEquals(
                        404,
                        assertThrows(FileStorageException.class, () -> storage.read(url))
                                .getCode());
            }
        } finally {
            server.stop(0);
        }
    }
}
