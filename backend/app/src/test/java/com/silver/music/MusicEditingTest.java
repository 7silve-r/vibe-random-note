package com.silver.music;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.silver.account.utils.JwtUtil;
import com.silver.storage.model.UploadTarget;
import com.silver.storage.service.FileStorage;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class MusicEditingTest {
    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtUtil jwt;
    @MockitoBean FileStorage storage;
    MockMvc mvc;
    String admin;

    @BeforeEach
    void setup() {
        jdbc.update(
                "INSERT INTO account_user (id,username,role) VALUES (1931,'edit_admin','ADMIN'),(1932,'edit_reader','USER')");
        jdbc.update("INSERT INTO music_artist (id,name) VALUES (1931,'甲歌手'),(1932,'乙歌手')");
        jdbc.update("INSERT INTO music_style (id,name) VALUES (1931,'测试流行'),(1932,'测试民谣')");
        jdbc.update(
                "INSERT INTO music_song (id,name,artist_id,album,release_time,lyric,lyric_url) VALUES (2931,'合唱',1931,'专辑','2026-10-17','旧歌词','old.lrc')");
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        admin = "Bearer " + jwt.generateToken("edit_admin");
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM music_song WHERE artist_id IN (1931,1932)");
        jdbc.update("DELETE FROM music_artist WHERE id IN (1931,1932)");
        jdbc.update("DELETE FROM music_style WHERE id IN (1931,1932)");
        jdbc.update("DELETE FROM account_user WHERE id IN (1931,1932)");
    }

    String update(String artists, String styles) {
        return "{\"songId\":2931,\"songName\":\"合唱\",\"album\":\"专辑\",\"releaseTime\":\"2026-10-17\",\"artistIds\":"
                + artists
                + ",\"styleIds\":"
                + styles
                + "}";
    }

    @Test
    void multipleArtistsAndStylesRoundTripWithoutDuplicateSongs() throws Exception {
        mvc.perform(
                        put("/music/admin/updateSong")
                                .header("Authorization", admin)
                                .contentType("application/json")
                                .content(update("[1932,1931,1932]", "[1931,1932,1931]")))
                .andExpect(status().isOk());
        mvc.perform(get("/music/public/song/getSongDetail/2931"))
                .andExpect(jsonPath("$.data.artistName").value("乙歌手 / 甲歌手"))
                .andExpect(jsonPath("$.data.artistIds[0]").value(1932))
                .andExpect(jsonPath("$.data.artists.length()").value(2))
                .andExpect(jsonPath("$.data.styleIds.length()").value(2));
        for (String name : new String[] {"甲歌手", "乙歌手"})
            mvc.perform(
                            post("/music/public/song/listSongs")
                                    .contentType("application/json")
                                    .content("{\"artistName\":\"" + name + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.total").value(1));
        for (int id : new int[] {1931, 1932})
            mvc.perform(get("/music/public/artist/getArtistDetail/" + id))
                    .andExpect(jsonPath("$.data.songs.length()").value(1));
        mvc.perform(
                        post("/music/admin/listAdminSongs")
                                .header("Authorization", admin)
                                .contentType("application/json")
                                .content("{\"artistId\":1931}"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].artistIds.length()").value(2));
        mvc.perform(delete("/music/admin/deleteArtist/1931").header("Authorization", admin))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        put("/music/admin/updateSong")
                                .header("Authorization", admin)
                                .contentType("application/json")
                                .content(update("[1932]", "[]")))
                .andExpect(status().isOk());
        assertEquals(
                0,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM music_genre WHERE song_id=2931", Integer.class));
        mvc.perform(delete("/music/admin/deleteArtist/1931").header("Authorization", admin))
                .andExpect(status().isOk());
        assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM music_song WHERE id=2931", Integer.class));
    }

    @Test
    void invalidAssociationsDoNotChangeSong() throws Exception {
        for (String body :
                new String[] {
                    update("[]", "[]"), update("[999999]", "[]"), update("[1931]", "[999999]")
                })
            mvc.perform(
                            put("/music/admin/updateSong")
                                    .header("Authorization", admin)
                                    .contentType("application/json")
                                    .content(body))
                    .andExpect(status().isBadRequest());
        assertEquals(
                1931L,
                jdbc.queryForObject("SELECT artist_id FROM music_song WHERE id=2931", Long.class));
    }

    MockMultipartFile lyric(String name, String content) {
        return new MockMultipartFile(
                "file", name, "text/plain", content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void uploadReadReplaceAndDeleteLyrics() throws Exception {
        when(storage.upload(any(), eq(UploadTarget.SONG_LYRIC))).thenReturn("new.lrc");
        mvc.perform(
                        multipart("/music/admin/songs/2931/lyric")
                                .file(lyric("song.lrc", "\uFEFF[00:01.00]第一句\r\n[00:03.00]第二句"))
                                .with(
                                        r -> {
                                            r.setMethod("PUT");
                                            return r;
                                        })
                                .header("Authorization", admin))
                .andExpect(status().isOk());
        verify(storage).deleteQuietly("old.lrc");
        mvc.perform(get("/music/public/song/2931/lyrics"))
                .andExpect(jsonPath("$.data.lyric").value("[00:01.00]第一句\n[00:03.00]第二句"))
                .andExpect(jsonPath("$.data.lyricUrl").value("new.lrc"));
        mvc.perform(delete("/music/admin/songs/2931/lyric").header("Authorization", admin))
                .andExpect(status().isOk());
        verify(storage).deleteQuietly("new.lrc");
        assertNull(jdbc.queryForObject("SELECT lyric FROM music_song WHERE id=2931", String.class));
        assertNull(
                jdbc.queryForObject(
                        "SELECT lyric_url FROM music_song WHERE id=2931", String.class));
    }

    @Test
    void failedUploadCleansOnlyNewObjectAndPermissionsAreChecked() throws Exception {
        when(storage.upload(any(), eq(UploadTarget.SONG_LYRIC))).thenReturn("orphan.lrc");
        mvc.perform(
                        multipart("/music/admin/songs/999999/lyric")
                                .file(lyric("song.txt", "正文"))
                                .with(
                                        r -> {
                                            r.setMethod("PUT");
                                            return r;
                                        })
                                .header("Authorization", admin))
                .andExpect(status().isNotFound());
        verify(storage).deleteQuietly("orphan.lrc");
        verify(storage, never()).deleteQuietly("old.lrc");
        mvc.perform(
                        multipart("/music/admin/songs/2931/lyric")
                                .file(lyric("song.lrc", "正文"))
                                .with(
                                        r -> {
                                            r.setMethod("PUT");
                                            return r;
                                        }))
                .andExpect(status().isUnauthorized());
        mvc.perform(
                        delete("/music/admin/songs/2931/lyric")
                                .header(
                                        "Authorization",
                                        "Bearer " + jwt.generateToken("edit_reader")))
                .andExpect(status().isForbidden());
        mvc.perform(
                        multipart("/music/admin/songs/2931/lyric")
                                .file(lyric("file.exe", "not lyrics"))
                                .with(
                                        r -> {
                                            r.setMethod("PUT");
                                            return r;
                                        })
                                .header("Authorization", admin))
                .andExpect(status().isUnsupportedMediaType());
    }
}
