package com.silver.music;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.silver.account.utils.JwtUtil;
import com.silver.storage.model.UploadTarget;
import com.silver.storage.service.FileStorage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties =
                "spring.datasource.url=jdbc:h2:mem:upload-http;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class UploadHttpTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtUtil jwt;

    @Value("${local.server.port}")
    int port;

    @MockitoBean FileStorage storage;

    @BeforeEach
    void setup() {
        jdbc.update(
                "INSERT INTO `account_user` (id, username, role) VALUES (901, 'upload_admin', 'ADMIN')");
        jdbc.update("INSERT INTO music_artist (id, name) VALUES (901, 'upload artist')");
        jdbc.update(
                "INSERT INTO music_song (id, name, artist_id, album, release_time) VALUES (901, 'upload song', 901, 'test album', CURRENT_DATE)");
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM music_song WHERE id = 901");
        jdbc.update("DELETE FROM music_artist WHERE id = 901");
        jdbc.update("DELETE FROM `account_user` WHERE id = 901");
    }

    private HttpResponse<String> send(int megabytes) throws Exception {
        String boundary = "vibe-upload-test";
        byte[] bytes = new byte[megabytes * 1024 * 1024];
        bytes[0] = 'I';
        bytes[1] = 'D';
        bytes[2] = '3';
        String start =
                "--"
                        + boundary
                        + "\r\nContent-Disposition: form-data; name=\"duration\"\r\n\r\n120\r\n"
                        + "--"
                        + boundary
                        + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"test.mp3\"\r\nContent-Type: audio/mpeg\r\n\r\n";
        var body =
                HttpRequest.BodyPublishers.concat(
                        HttpRequest.BodyPublishers.ofString(start, StandardCharsets.UTF_8),
                        HttpRequest.BodyPublishers.ofByteArray(bytes),
                        HttpRequest.BodyPublishers.ofString("\r\n--" + boundary + "--\r\n"));
        var request =
                HttpRequest.newBuilder(
                                URI.create(
                                        "http://127.0.0.1:"
                                                + port
                                                + "/music/admin/songs/901/audio"))
                        .timeout(Duration.ofSeconds(30))
                        .header("Authorization", "Bearer " + jwt.generateToken("upload_admin"))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .PUT(body)
                        .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void audio() throws Exception {
        when(storage.upload(any(), eq(UploadTarget.SONG_AUDIO)))
                .thenAnswer(
                        call -> {
                            org.springframework.web.multipart.MultipartFile file =
                                    call.getArgument(0);
                            assertEquals(10 * 1024 * 1024, file.getSize());
                            return "http://storage.invalid/test.mp3";
                        });
        var response = send(10);
        assertEquals(200, response.statusCode(), response.body());
        assertEquals(
                "http://storage.invalid/test.mp3",
                jdbc.queryForObject(
                        "SELECT audio_url FROM music_song WHERE id = 901", String.class));
        verify(storage).upload(any(), eq(UploadTarget.SONG_AUDIO));
    }

    @Test
    void oversized() throws Exception {
        var response = send(51);
        assertEquals(413, response.statusCode(), response.body());
        assertTrue(response.body().contains("413"));
        verifyNoInteractions(storage);
    }
}
