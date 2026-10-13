package com.silver.music;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.silver.account.utils.JwtUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class MusicTest {
    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtUtil jwt;

    @org.springframework.beans.factory.annotation.Value("${local.server.port}")
    int port;

    MockMvc mvc;

    @BeforeEach
    void setup() {
        jdbc.update(
                "INSERT INTO `user` (id, username, role) VALUES (1, 'writer01', 'USER'), (2, 'ADMIN', 'ADMIN')");
        jdbc.update("INSERT INTO tb_artist (id, name) VALUES (1, '歌手')");
        jdbc.update(
                "INSERT INTO tb_song (id, name, artist_id, album, release_time) VALUES (1, '歌曲', 1, '专辑', CURRENT_DATE)");
        jdbc.update("INSERT INTO tb_playlist (id, title) VALUES (1, '歌单')");
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void guestSong() throws Exception {
        mvc.perform(get("/music/public/song/getSongDetail/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.songName").value("歌曲"));
    }

    @Test
    void sharedUser() throws Exception {
        String token = jwt.generateToken("writer01");
        mvc.perform(get("/my/profile").header("Authorization", token)).andExpect(status().isOk());
        mvc.perform(
                        post("/music/favorite/collectSong")
                                .param("songId", "1")
                                .header("Authorization", token))
                .andExpect(status().isOk());
    }

    @Test
    void guestWrite() throws Exception {
        mvc.perform(post("/music/favorite/collectSong").param("songId", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void noAdmin() throws Exception {
        mvc.perform(
                        delete("/music/admin/deleteSong/1")
                                .header("Authorization", jwt.generateToken("writer01")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void adminUser() throws Exception {
        mvc.perform(
                        post("/music/favorite/collectSong")
                                .param("songId", "1")
                                .header("Authorization", jwt.generateToken("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void adminDelete() throws Exception {
        mvc.perform(
                        delete("/music/admin/deleteSong/1")
                                .header("Authorization", jwt.generateToken("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void repeatFavorite() throws Exception {
        for (int i = 0; i < 2; i++)
            mvc.perform(
                            post("/music/favorite/collectSong")
                                    .param("songId", "1")
                                    .header("Authorization", jwt.generateToken("writer01")))
                    .andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(
                1, jdbc.queryForObject("SELECT COUNT(*) FROM tb_user_favorite", Integer.class));
    }

    @Test
    void commentLikes() throws Exception {
        jdbc.update(
                "INSERT INTO tb_comment (id, user_id, song_id, content, create_time, type, like_count) VALUES (10, 1, 1, 'hi', CURRENT_TIMESTAMP, 0, 0)");
        for (int i = 0; i < 2; i++)
            mvc.perform(
                            patch("/music/comment/likeComment/10")
                                    .header("Authorization", jwt.generateToken("ADMIN")))
                    .andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT like_count FROM tb_comment WHERE id = 10", Integer.class));
        for (int i = 0; i < 2; i++)
            mvc.perform(
                            patch("/music/comment/cancelLikeComment/10")
                                    .header("Authorization", jwt.generateToken("ADMIN")))
                    .andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(
                0,
                jdbc.queryForObject(
                        "SELECT like_count FROM tb_comment WHERE id = 10", Integer.class));
    }

    @Test
    void foreignComment() throws Exception {
        jdbc.update(
                "INSERT INTO tb_comment (id, user_id, song_id, content, create_time, type, like_count) VALUES (10, 2, 1, 'hi', CURRENT_TIMESTAMP, 0, 0)");
        mvc.perform(
                        delete("/music/comment/deleteComment/10")
                                .header("Authorization", jwt.generateToken("writer01")))
                .andExpect(status().isForbidden());
        mvc.perform(
                        delete("/music/comment/deleteComment/10")
                                .header("Authorization", jwt.generateToken("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void missing() throws Exception {
        mvc.perform(get("/music/public/song/getSongDetail/999")).andExpect(status().isNotFound());
        mvc.perform(get("/music/public/playlist/getPlaylistDetail/999"))
                .andExpect(status().isNotFound());
        mvc.perform(
                        post("/music/favorite/collectSong")
                                .param("songId", "999")
                                .header("Authorization", jwt.generateToken("writer01")))
                .andExpect(status().isNotFound());
    }

    @Test
    void badComment() throws Exception {
        mvc.perform(
                        post("/music/comment/addSongComment")
                                .header("Authorization", jwt.generateToken("writer01"))
                                .contentType("application/json")
                                .content("{\"songId\":1,\"content\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void favoritePage() throws Exception {
        jdbc.update(
                "INSERT INTO tb_user_favorite (user_id, song_id, type, create_time) VALUES (1,1,0,CURRENT_TIMESTAMP), (2,1,0,CURRENT_TIMESTAMP)");
        mvc.perform(
                        post("/music/favorite/getFavoriteSongs")
                                .header("Authorization", jwt.generateToken("writer01"))
                                .contentType("application/json")
                                .content("{\"pageNum\":1,\"pageSize\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void oneServer() throws Exception {
        var client = java.net.http.HttpClient.newHttpClient();
        for (String path : new String[] {"/public/articles", "/music/public/styles"}) {
            var request =
                    java.net.http.HttpRequest.newBuilder(
                                    java.net.URI.create("http://127.0.0.1:" + port + path))
                            .GET()
                            .build();
            var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            org.junit.jupiter.api.Assertions.assertEquals(200, response.statusCode());
        }
    }

    @Test
    void binding() throws Exception {
        mvc.perform(
                        put("/music/admin/playlists/1/songs")
                                .header("Authorization", jwt.generateToken("ADMIN"))
                                .contentType("application/json")
                                .content("[1,1]"))
                .andExpect(status().isOk());
        mvc.perform(get("/music/public/playlist/getPlaylistDetail/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.songs.length()").value(1));
        mvc.perform(
                        put("/music/admin/playlists/1/songs")
                                .header("Authorization", jwt.generateToken("ADMIN"))
                                .contentType("application/json")
                                .content("[999]"))
                .andExpect(status().isNotFound());
        org.junit.jupiter.api.Assertions.assertEquals(
                1, jdbc.queryForObject("SELECT COUNT(*) FROM tb_playlist_binding", Integer.class));
    }

    @Test
    void adminAdd() throws Exception {
        mvc.perform(
                        post("/music/admin/addSong")
                                .header("Authorization", jwt.generateToken("ADMIN"))
                                .contentType("application/json")
                                .content(
                                        "{\"artistId\":1,\"songName\":\"新歌\",\"album\":\"专辑\",\"releaseTime\":\"2026-10-03\"}"))
                .andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(
                2, jdbc.queryForObject("SELECT COUNT(*) FROM tb_song", Integer.class));
    }

    @Test
    void uploadDenied() throws Exception {
        var file =
                new org.springframework.mock.web.MockMultipartFile(
                        "file", "x.wav", "audio/wav", new byte[1]);
        mvc.perform(
                        multipart("/music/admin/songs/1/audio")
                                .file(file)
                                .param("duration", "1")
                                .with(
                                        request -> {
                                            request.setMethod("PUT");
                                            return request;
                                        })
                                .header("Authorization", jwt.generateToken("writer01")))
                .andExpect(status().isForbidden());
    }

    @Test
    void favoriteState() throws Exception {
        String token = jwt.generateToken("writer01");
        mvc.perform(
                        post("/music/favorite/collectSong")
                                .param("songId", "1")
                                .header("Authorization", token))
                .andExpect(status().isOk());
        mvc.perform(get("/music/public/song/getSongDetail/1").header("Authorization", token))
                .andExpect(jsonPath("$.data.collected").value(true))
                .andExpect(jsonPath("$.data.likeStatus").doesNotExist());
        mvc.perform(get("/music/public/song/getSongDetail/1"))
                .andExpect(jsonPath("$.data.collected").value(false));
        mvc.perform(
                        delete("/music/favorite/cancelCollectSong")
                                .param("songId", "1")
                                .header("Authorization", token))
                .andExpect(status().isOk());
        mvc.perform(get("/music/public/song/getSongDetail/1").header("Authorization", token))
                .andExpect(jsonPath("$.data.collected").value(false));
        mvc.perform(
                        post("/music/favorite/collectPlaylist")
                                .param("playlistId", "1")
                                .header("Authorization", token))
                .andExpect(status().isOk());
        mvc.perform(
                        get("/music/public/playlist/getPlaylistDetail/1")
                                .header("Authorization", token))
                .andExpect(jsonPath("$.data.collected").value(true));
        mvc.perform(get("/music/public/playlist/getPlaylistDetail/1"))
                .andExpect(jsonPath("$.data.collected").value(false));
    }

    @Test
    void booleanBannerState() throws Exception {
        jdbc.update("INSERT INTO tb_banner (id, banner_url, status) VALUES (1, '/banner.png', 0)");
        String token = jwt.generateToken("ADMIN");
        mvc.perform(
                        post("/music/admin/listBanners")
                                .header("Authorization", token)
                                .contentType("application/json")
                                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].enabled").value(true))
                .andExpect(jsonPath("$.data.list[0].status").doesNotExist());
        mvc.perform(
                        patch("/music/admin/updateBannerStatus/1")
                                .header("Authorization", token)
                                .param("enabled", "false"))
                .andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(
                1, jdbc.queryForObject("SELECT status FROM tb_banner WHERE id = 1", Integer.class));
        mvc.perform(get("/music/public/banner/getBannerList"))
                .andExpect(jsonPath("$.data").isEmpty());
        mvc.perform(
                        post("/music/admin/listBanners")
                                .header("Authorization", token)
                                .contentType("application/json")
                                .content("{\"enabled\":false}"))
                .andExpect(jsonPath("$.data.list[0].enabled").value(false));
        mvc.perform(
                        patch("/music/admin/updateBannerStatus/1")
                                .header("Authorization", token)
                                .param("enabled", "true"))
                .andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(
                0, jdbc.queryForObject("SELECT status FROM tb_banner WHERE id = 1", Integer.class));
        mvc.perform(get("/music/public/banner/getBannerList"))
                .andExpect(jsonPath("$.data[0].enabled").value(true));
        mvc.perform(
                        patch("/music/admin/updateBannerStatus/1")
                                .header("Authorization", token)
                                .param("enabled", "invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void emptyPage() throws Exception {
        mvc.perform(
                        post("/music/public/song/listSongs")
                                .contentType("application/json")
                                .content("{\"pageNum\":99,\"pageSize\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list").isEmpty());
    }

    @Test
    void recommendStyle() throws Exception {
        jdbc.update("UPDATE tb_playlist SET style = '轻音乐' WHERE id = 1");
        jdbc.update("INSERT INTO tb_playlist (id, title, style) VALUES (2, '同风格', '轻音乐')");
        mvc.perform(
                        post("/music/favorite/collectPlaylist")
                                .param("playlistId", "1")
                                .header("Authorization", jwt.generateToken("writer01")))
                .andExpect(status().isOk());
        mvc.perform(
                        get("/music/public/playlist/getRecommendedPlaylists")
                                .header("Authorization", jwt.generateToken("writer01")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].playlistId").value(2));
    }

    @Test
    void noArtists() throws Exception {
        jdbc.update("DELETE FROM tb_song");
        jdbc.update("DELETE FROM tb_artist");
        mvc.perform(
                        get("/music/admin/listArtistNames")
                                .header("Authorization", jwt.generateToken("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void allStyles() throws Exception {
        jdbc.update("UPDATE tb_playlist SET style = '流行' WHERE id = 1");
        jdbc.update(
                "INSERT INTO tb_playlist (id, title, style) VALUES (2, '摇滚歌单', '摇滚'), (3, '未分类', NULL)");
        for (String style : new String[] {"null", "\"\"", "\"   \""}) {
            String body = "{\"pageNum\":1,\"pageSize\":10,\"style\":" + style + "}";
            mvc.perform(
                            post("/music/public/playlist/listPlaylists")
                                    .contentType("application/json")
                                    .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.total").value(3))
                    .andExpect(jsonPath("$.data.list.length()").value(3));
            mvc.perform(
                            post("/music/admin/listPlaylists")
                                    .header("Authorization", jwt.generateToken("ADMIN"))
                                    .contentType("application/json")
                                    .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.total").value(3));
        }
        mvc.perform(
                        post("/music/public/playlist/listPlaylists")
                                .contentType("application/json")
                                .content("{\"style\":\"流行\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].playlistId").value(1));
    }

    @Test
    void details() throws Exception {
        jdbc.update(
                "INSERT INTO tb_song (id, name, artist_id, album, release_time, cover_url) VALUES (2, '另一首', 1, '专辑', CURRENT_DATE, '/song.png')");
        jdbc.update("UPDATE tb_playlist SET cover_url = '/playlist.png' WHERE id = 1");
        jdbc.update("INSERT INTO tb_playlist_binding (playlist_id, song_id) VALUES (1, 1), (1, 2)");
        jdbc.update(
                "INSERT INTO tb_comment (user_id, playlist_id, content, type, like_count, create_time) VALUES (1, 1, '评论一', 1, 2, CURRENT_TIMESTAMP), (2, 1, '评论二', 1, 3, CURRENT_TIMESTAMP)");
        jdbc.update(
                "INSERT INTO tb_comment (user_id, song_id, content, type, like_count, create_time) VALUES (1, 2, '歌曲评论', 0, 4, CURRENT_TIMESTAMP)");
        mvc.perform(get("/music/public/playlist/getPlaylistDetail/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.coverUrl").value("/playlist.png"))
                .andExpect(jsonPath("$.data.songs.length()").value(2))
                .andExpect(jsonPath("$.data.songs[1].coverUrl").value("/song.png"))
                .andExpect(jsonPath("$.data.comments.length()").value(2))
                .andExpect(jsonPath("$.data.comments[0].username").value("writer01"))
                .andExpect(jsonPath("$.data.comments[1].likeCount").value(3));
        mvc.perform(get("/music/public/artist/getArtistDetail/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.artistName").value("歌手"))
                .andExpect(jsonPath("$.data.songs.length()").value(2));
        mvc.perform(get("/music/public/song/getSongDetail/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.coverUrl").value("/song.png"))
                .andExpect(jsonPath("$.data.comments.length()").value(1))
                .andExpect(jsonPath("$.data.comments[0].content").value("歌曲评论"));
    }

    @Test
    void emptyDetails() throws Exception {
        jdbc.update("INSERT INTO tb_artist (id, name) VALUES (2, '空歌手')");
        mvc.perform(get("/music/public/artist/getArtistDetail/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.songs").isArray())
                .andExpect(jsonPath("$.data.songs").isEmpty());
        mvc.perform(get("/music/public/playlist/getPlaylistDetail/1"))
                .andExpect(jsonPath("$.data.songs").isEmpty())
                .andExpect(jsonPath("$.data.comments").isEmpty());
        mvc.perform(get("/music/public/song/getSongDetail/1"))
                .andExpect(jsonPath("$.data.comments").isEmpty());
        mvc.perform(get("/music/public/artist/getArtistDetail/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void favoriteFilter() throws Exception {
        jdbc.update("INSERT INTO tb_playlist (id, title, style) VALUES (2, '轻音', '流行')");
        jdbc.update(
                "INSERT INTO tb_user_favorite (user_id, playlist_id, type, create_time) VALUES (1,1,1,'2026-01-01'), (1,2,1,'2026-01-02'), (2,2,1,'2026-01-03')");
        mvc.perform(
                        post("/music/favorite/getFavoritePlaylists")
                                .header("Authorization", jwt.generateToken("writer01"))
                                .contentType("application/json")
                                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list[0].playlistId").value(2));
        mvc.perform(
                        post("/music/favorite/getFavoritePlaylists")
                                .header("Authorization", jwt.generateToken("writer01"))
                                .contentType("application/json")
                                .content(
"""
{"title":"轻","style":"流行"}
"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void songStyles() throws Exception {
        String token = jwt.generateToken("ADMIN");
        jdbc.update("INSERT INTO tb_style (id, name) VALUES (1, '流行'), (2, '轻音乐')");
        mvc.perform(
                        post("/music/admin/addSong")
                                .header("Authorization", token)
                                .contentType("application/json")
                                .content(
"""
{"artistId":1,"songName":"新歌","album":"专辑","style":"流行,轻音乐","releaseTime":"2026-10-06"}
"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        Long id = jdbc.queryForObject("SELECT id FROM tb_song WHERE name = '新歌'", Long.class);
        org.junit.jupiter.api.Assertions.assertEquals(
                2,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM tb_genre WHERE song_id = ?", Integer.class, id));
        jdbc.update("INSERT INTO tb_genre (song_id, style_id) VALUES (1, 1)");
        mvc.perform(
                        post("/music/favorite/collectSong")
                                .param("songId", "1")
                                .header("Authorization", token))
                .andExpect(status().isOk());
        mvc.perform(get("/music/public/song/getRecommendedSongs").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].songId").value(id));
        mvc.perform(
                        put("/music/admin/updateSong")
                                .header("Authorization", token)
                                .contentType("application/json")
                                .content(
                                        "{\"songId\":"
                                                + id
                                                + ",\"artistId\":1,\"songName\":\"新歌\",\"album\":\"专辑\",\"style\":\"轻音乐\",\"releaseTime\":\"2026-10-06\"}"))
                .andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM tb_genre WHERE song_id = ?", Integer.class, id));
    }

    @Test
    void adminQueries() throws Exception {
        String token = jwt.generateToken("ADMIN");
        jdbc.update("UPDATE tb_artist SET gender = 0, area = '中国' WHERE id = 1");
        mvc.perform(
                        post("/music/admin/listArtists")
                                .header("Authorization", token)
                                .contentType("application/json")
                                .content(
"""
{"artistName":"歌","gender":0,"area":"中国"}
"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(
                        post("/music/admin/listAdminSongs")
                                .header("Authorization", token)
                                .contentType("application/json")
                                .content(
"""
{"artistId":1,"songName":"歌曲","album":"专辑"}
"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].artistName").value("歌手"));
        jdbc.update(
                "INSERT INTO tb_banner (id, banner_url, status) VALUES (1, '/banner.png', 0), (2, '/hidden.png', 1)");
        mvc.perform(get("/music/public/banner/getBannerList"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
        mvc.perform(
                        post("/music/admin/listBanners")
                                .header("Authorization", token)
                                .contentType("application/json")
                                .content(
"""
{"enabled":false}
"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].bannerId").value(2));
    }
}
