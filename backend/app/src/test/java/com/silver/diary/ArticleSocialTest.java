package com.silver.diary;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.silver.account.utils.JwtUtil;
import com.silver.storage.service.FileStorage;
import java.io.InputStream;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ArticleSocialTest {
    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtUtil jwt;
    @Autowired com.silver.diary.mapper.ArticleMapper articles;
    @MockitoBean FileStorage storage;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        jdbc.update("DELETE FROM diary_article_comment_like");
        jdbc.update("DELETE FROM diary_article_comment");
        jdbc.update("DELETE FROM diary_article_like");
        jdbc.update("DELETE FROM diary_article_favorite");
        jdbc.update("DELETE FROM diary_article");
        jdbc.update("DELETE FROM diary_category");
        jdbc.update("DELETE FROM `account_user`");
        jdbc.update(
                "INSERT INTO `account_user` (id, username, role) VALUES (1, 'owner01', 'USER'), (2, 'reader01', 'USER'), (3, 'ADMIN', 'ADMIN')");
        jdbc.update("INSERT INTO diary_category (id, cate_name, create_user) VALUES (1, '生活', 1)");
        jdbc.update(
                "INSERT INTO diary_article (id, title, content, state, create_user, cate_id) VALUES (10, '公开标题', '公开正文', '公开', 1, 1), (11, '私有标题', '私有正文', '私有', 1, 1)");
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    String token(String user) {
        return jwt.generateToken(user);
    }

    @Test
    void coverReadPermissions() throws Exception {
        String key = "covers/12345678-1234-1234-1234-123456789012.png";
        String url = "/uploads/" + key;
        jdbc.update("UPDATE diary_article SET cover_img = ? WHERE id = 11", url);
        byte[] bytes = {1, 2, 3};
        var object = mock(InputStream.class);
        when(object.readAllBytes()).thenReturn(bytes);
        when(storage.read("/uploads/" + key)).thenReturn(object);
        mvc.perform(get(url)).andExpect(status().isNotFound());
        mvc.perform(get(url).header("Authorization", token("reader01")))
                .andExpect(status().isNotFound());
        verifyNoInteractions(storage);
        mvc.perform(get(url).header("Authorization", token("owner01")))
                .andExpect(status().isOk())
                .andExpect(content().bytes(bytes))
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("Cache-Control", "no-store"));
        jdbc.update("UPDATE diary_article SET state = '公开' WHERE id = 11");
        mvc.perform(get(url)).andExpect(status().isOk());
        jdbc.update("UPDATE diary_article SET state = '私有' WHERE id = 11");
        mvc.perform(get(url)).andExpect(status().isNotFound());
        verify(storage, times(2)).read("/uploads/" + key);
        verify(object, times(2)).close();
    }

    @Test
    void avatarRead() throws Exception {
        String key = "avatars/12345678-1234-1234-1234-123456789012.jpg";
        var object = mock(InputStream.class);
        when(object.readAllBytes()).thenReturn(new byte[] {1});
        when(storage.read("/uploads/" + key)).thenReturn(object);
        mvc.perform(get("/uploads/" + key))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"));
        verify(object).close();
    }

    @Test
    void publicList() throws Exception {
        mvc.perform(get("/public/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(10));
    }

    @Test
    void ownData() throws Exception {
        mvc.perform(get("/my/categories")).andExpect(status().isUnauthorized());
        mvc.perform(get("/my/categories").header("Authorization", token("owner01")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].createUser").value(1));
        mvc.perform(get("/my/categories").header("Authorization", token("reader01")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
        mvc.perform(
                        get("/my/article/list")
                                .param("pageNum", "1")
                                .param("pageSize", "10")
                                .header("Authorization", token("owner01")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));
        mvc.perform(
                        get("/my/article/list")
                                .param("pageNum", "1")
                                .param("pageSize", "10")
                                .header("Authorization", token("reader01")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
        mvc.perform(
                        get("/my/article/info")
                                .param("id", "11")
                                .header("Authorization", token("owner01")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(11));
        mvc.perform(
                        get("/my/article/info")
                                .param("id", "11")
                                .header("Authorization", token("reader01")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ownerFromIdentity() throws Exception {
        mvc.perform(
                        post("/my/categories")
                                .header("Authorization", token("reader01"))
                                .contentType("application/json")
                                .content(
                                        "{\"cateName\":\"读书\",\"cateAlias\":\"read\",\"createUser\":1}"))
                .andExpect(status().isOk());
        Integer categoryId =
                jdbc.queryForObject(
                        "SELECT id FROM diary_category WHERE create_user = 2", Integer.class);
        mvc.perform(
                        multipart("/my/article")
                                .header("Authorization", token("reader01"))
                                .param("title", "新日记")
                                .param("content", "正文")
                                .param("cateId", categoryId.toString())
                                .param("state", "私有")
                                .param("createUser", "1"))
                .andExpect(status().isOk());
        assertEquals(
                2,
                jdbc.queryForObject(
                        "SELECT create_user FROM diary_article WHERE cate_id = ?",
                        Integer.class,
                        categoryId));
        mvc.perform(
                        multipart("/my/article")
                                .header("Authorization", token("reader01"))
                                .param("title", "不能使用别人的分类")
                                .param("content", "正文")
                                .param("cateId", "1")
                                .param("state", "私有"))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        delete("/my/article")
                                .param("id", "11")
                                .header("Authorization", token("reader01")))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        delete("/my/categories")
                                .param("id", categoryId.toString())
                                .header("Authorization", token("owner01")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void privateRead() throws Exception {
        mvc.perform(get("/public/articles/11")).andExpect(status().isNotFound());
        mvc.perform(get("/public/articles/11").header("Authorization", token("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void guestWrite() throws Exception {
        mvc.perform(post("/my/article/10/like")).andExpect(status().isUnauthorized());
        mvc.perform(
                        post("/my/article/10/comment")
                                .contentType("application/json")
                                .content("{\"content\":\"hi\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void likes() throws Exception {
        for (int i = 0; i < 2; i++)
            mvc.perform(post("/my/article/10/like").header("Authorization", token("reader01")))
                    .andExpect(status().isOk());
        mvc.perform(get("/public/articles/10")).andExpect(jsonPath("$.data.likeCount").value(1));
        for (int i = 0; i < 2; i++)
            mvc.perform(delete("/my/article/10/like").header("Authorization", token("reader01")))
                    .andExpect(status().isOk());
        mvc.perform(get("/public/articles/10")).andExpect(jsonPath("$.data.likeCount").value(0));
    }

    @Test
    void hideFavorite() throws Exception {
        mvc.perform(post("/my/article/10/favorite").header("Authorization", token("reader01")))
                .andExpect(status().isOk());
        var update = new com.silver.diary.entity.Article();
        update.setId(10);
        update.setState("私有");
        articles.updateById(update);
        mvc.perform(get("/my/article/favorites").header("Authorization", token("reader01")))
                .andExpect(jsonPath("$.data.total").value(0));
        mvc.perform(post("/my/article/10/like").header("Authorization", token("reader01")))
                .andExpect(status().isNotFound());
        update.setState("公开");
        articles.updateById(update);
        mvc.perform(get("/my/article/favorites").header("Authorization", token("reader01")))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void comments() throws Exception {
        mvc.perform(
                        post("/my/article/10/comment")
                                .header("Authorization", token("reader01"))
                                .contentType("application/json")
                                .content("{\"content\":\"喜欢这篇日记\"}"))
                .andExpect(status().isOk());
        Integer id = jdbc.queryForObject("SELECT id FROM diary_article_comment", Integer.class);
        mvc.perform(delete("/my/article/comments/" + id).header("Authorization", token("owner01")))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/my/article/comments/" + id).header("Authorization", token("reader01")))
                .andExpect(status().isOk());
        assertEquals(
                0,
                jdbc.queryForObject("SELECT COUNT(*) FROM diary_article_comment", Integer.class));
    }

    @Test
    void commentLikes() throws Exception {
        jdbc.update(
                "INSERT INTO diary_article_comment (id, article_id, user_id, content, create_time) VALUES (5, 10, 2, 'hi', CURRENT_TIMESTAMP)");
        for (int i = 0; i < 2; i++)
            mvc.perform(
                            post("/my/article/comments/5/like")
                                    .header("Authorization", token("owner01")))
                    .andExpect(status().isOk());
        mvc.perform(get("/public/articles/10/comments"))
                .andExpect(jsonPath("$.data.list[0].likeCount").value(1));
        mvc.perform(delete("/my/article/comments/5").header("Authorization", token("ADMIN")))
                .andExpect(status().isOk());
        assertEquals(
                0,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM diary_article_comment_like", Integer.class));
    }

    @Test
    void privateComment() throws Exception {
        jdbc.update(
                "INSERT INTO diary_article_comment (id, article_id, user_id, content, create_time) VALUES (5, 11, 2, 'hi', CURRENT_TIMESTAMP)");
        mvc.perform(get("/public/articles/11/comments")).andExpect(status().isNotFound());
        mvc.perform(delete("/my/article/comments/5").header("Authorization", token("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminDelete() throws Exception {
        mvc.perform(post("/my/article/10/favorite").header("Authorization", token("reader01")))
                .andExpect(status().isOk());
        mvc.perform(delete("/admin/diary/articles/10").header("Authorization", token("reader01")))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/admin/diary/articles/10").header("Authorization", token("ADMIN")))
                .andExpect(status().isOk());
        assertEquals(
                0,
                jdbc.queryForObject("SELECT COUNT(*) FROM diary_article_favorite", Integer.class));
        mvc.perform(delete("/admin/diary/articles/11").header("Authorization", token("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void privateCover() throws Exception {
        String name = "12345678-1234-1234-1234-123456789012.png";
        jdbc.update(
                "UPDATE diary_article SET cover_img = ? WHERE id = 11", "/uploads/covers/" + name);
        mvc.perform(get("/uploads/covers/" + name)).andExpect(status().isNotFound());
        mvc.perform(get("/uploads/covers/" + name).header("Authorization", token("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void badComment() throws Exception {
        mvc.perform(
                        post("/my/article/10/comment")
                                .header("Authorization", token("reader01"))
                                .contentType("application/json")
                                .content("{\"content\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectOwner() throws Exception {
        mvc.perform(delete("/admin/users/1").header("Authorization", token("ADMIN")))
                .andExpect(status().isForbidden());
        assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM `account_user` WHERE id = 1", Integer.class));
    }
}
