package com.silver.poetry;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.silver.account.utils.JwtUtil;
import com.silver.poetry.service.PoetryService;
import com.silver.storage.service.FileStorage;
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
class PoetryTest {
    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtUtil jwt;
    @Autowired PoetryService poetry;
    @Autowired com.silver.poetry.mapper.PoemMapper poems;
    @MockitoBean FileStorage storage;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        jdbc.update("DELETE FROM poetry_favorite");
        jdbc.update("DELETE FROM poetry_poem");
        jdbc.update("DELETE FROM poetry_author");
        jdbc.update(
                "INSERT INTO account_user (id, username, role) VALUES (801, 'poet01', 'USER'), (802, 'poet02', 'USER')");
        jdbc.update(
                "INSERT INTO poetry_author (id,source_key,name,original_name,dynasty,description) VALUES (1,'wang','王维','王維','唐','山水田园诗人'),(2,'su','苏轼','蘇軾','宋','宋代词人')");
        jdbc.update(
                "INSERT INTO poetry_poem (id,source_key,dataset,source_ref,source_version,author_id,author_name,title,original_title,dynasty,kind,content,original_content,excerpt,featured_rank) VALUES (1,'a','tang','全唐诗/poet.tang.0.json','local',1,'王维','相思','相思','唐','诗',?,?,'愿君多采撷，此物最相思。',1),(2,'b','song-ci','宋词/ci.song.0.json','local',2,'苏轼','定风波','定風波','宋','词','莫听穿林打叶声。','莫聽穿林打葉聲。','莫听穿林打叶声。',2)",
                "红豆生南国，春来发几枝。\n愿君多采撷，此物最相思。",
                "紅豆生南國，春來發幾枝。\n願君多采擷，此物最相思。");
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    String token(String user) {
        return "Bearer " + jwt.generateToken(user);
    }

    @Test
    void publicReadingAndFilters() throws Exception {
        mvc.perform(
                        get("/public/poetry")
                                .param("keyword", "王维")
                                .param("dynasty", "唐")
                                .param("kind", "诗"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(1));
        mvc.perform(get("/public/poetry").param("authorId", "2"))
                .andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(get("/public/poetry").param("keyword", "%"))
                .andExpect(jsonPath("$.data.total").value(0));
        mvc.perform(get("/public/poetry/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paragraphs.length()").value(2))
                .andExpect(jsonPath("$.data.authorDescription").value("山水田园诗人"))
                .andExpect(jsonPath("$.data.collected").value(false));
    }

    @Test
    void validationAndMissingPoem() throws Exception {
        mvc.perform(get("/public/poetry").param("pageSize", "999"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/public/poetry").param("pageNum", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/public/poetry").param("keyword", "诗".repeat(101)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/public/poetry/999")).andExpect(status().isNotFound());
        mvc.perform(put("/my/poetry/999/favorite").header("Authorization", token("poet01")))
                .andExpect(status().isNotFound());
    }

    @Test
    void favoritesRequireLoginAndAreIdempotentAndIsolated() throws Exception {
        mvc.perform(get("/my/poetry/favorites")).andExpect(status().isUnauthorized());
        mvc.perform(put("/my/poetry/1/favorite")).andExpect(status().isUnauthorized());
        for (int i = 0; i < 2; i++)
            mvc.perform(put("/my/poetry/1/favorite").header("Authorization", token("poet01")))
                    .andExpect(status().isOk());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM poetry_favorite", Integer.class));
        mvc.perform(get("/public/poetry/1").header("Authorization", token("poet01")))
                .andExpect(jsonPath("$.data.collected").value(true));
        mvc.perform(get("/my/poetry/favorites").header("Authorization", token("poet02")))
                .andExpect(jsonPath("$.data.total").value(0));
        mvc.perform(delete("/my/poetry/1/favorite").header("Authorization", token("poet02")))
                .andExpect(status().isOk());
        mvc.perform(get("/my/poetry/favorites").header("Authorization", token("poet01")))
                .andExpect(jsonPath("$.data.total").value(1));
        for (int i = 0; i < 2; i++)
            mvc.perform(delete("/my/poetry/1/favorite").header("Authorization", token("poet01")))
                    .andExpect(status().isOk());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM poetry_favorite", Integer.class));
    }

    @Test
    void dailyIsStableAndRandomExcludesPrevious() throws Exception {
        assertEquals(poetry.daily().id(), poetry.daily().id());
        mvc.perform(get("/public/poetry/daily"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.excerpt").isNotEmpty());
        mvc.perform(get("/public/poetry/random").param("exclude", "1"))
                .andExpect(jsonPath("$.data.id").value(2));
        poems.deleteById(2);
        assertEquals(1, poetry.random(1, null).id());
        poems.deleteById(1);
        mvc.perform(get("/public/poetry/random")).andExpect(status().isNotFound());
        mvc.perform(get("/public/poetry/daily")).andExpect(status().isNotFound());
    }

    @Test
    void favoriteCleanupOnAccountDeletion() throws Exception {
        mvc.perform(put("/my/poetry/1/favorite").header("Authorization", token("poet01")))
                .andExpect(status().isOk());
        jdbc.update("DELETE FROM account_user WHERE id=801");
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM poetry_favorite", Integer.class));
    }

    @Test
    void collectionsAreDiscoveredAndFilterAllReadingPaths() throws Exception {
        jdbc.update(
                "INSERT INTO poetry_author (id,source_key,name,original_name,dynasty) VALUES (3,'anonymous','佚名','佚名','先秦')");
        jdbc.update(
                "INSERT INTO poetry_poem (id,source_key,dataset,source_ref,source_version,author_id,author_name,title,original_title,dynasty,kind,content,original_content) VALUES (3,'c','shijing','诗经/shijing.json','local',3,'佚名','国风 · 周南 · 关雎','国风 · 周南 · 关雎','先秦','诗','关关雎鸠，在河之洲。','关关雎鸠，在河之洲。')");
        mvc.perform(get("/public/poetry/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.collections.length()").value(3))
                .andExpect(jsonPath("$.data.collections[2].code").value("shijing"))
                .andExpect(jsonPath("$.data.collections[2].name").value("诗经"))
                .andExpect(jsonPath("$.data.collections[2].total").value(1))
                .andExpect(jsonPath("$.data.dynasties", org.hamcrest.Matchers.hasItem("先秦")));
        mvc.perform(
                        get("/public/poetry")
                                .param("dataset", "shijing")
                                .param("kind", "诗")
                                .param("keyword", "关雎"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].dataset").value("shijing"));
        mvc.perform(get("/public/poetry").param("dataset", "shijing").param("dynasty", "唐"))
                .andExpect(jsonPath("$.data.total").value(0));
        mvc.perform(get("/public/poetry/3"))
                .andExpect(jsonPath("$.data.dataset").value("shijing"))
                .andExpect(jsonPath("$.data.paragraphs[0]").value("关关雎鸠，在河之洲。"));
        mvc.perform(get("/public/poetry/random").param("dataset", "shijing").param("exclude", "3"))
                .andExpect(jsonPath("$.data.id").value(3));
        mvc.perform(put("/my/poetry/3/favorite").header("Authorization", token("poet01")))
                .andExpect(status().isOk());
        mvc.perform(
                        get("/my/poetry/favorites")
                                .param("dataset", "shijing")
                                .header("Authorization", token("poet01")))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].collected").value(true));
        mvc.perform(
                        get("/my/poetry/favorites")
                                .param("dataset", "tang")
                                .header("Authorization", token("poet01")))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void emptyAndUnsupportedCollections() throws Exception {
        mvc.perform(get("/public/poetry").param("dataset", "yuanqu"))
                .andExpect(jsonPath("$.data.total").value(0));
        mvc.perform(get("/public/poetry/random").param("dataset", "yuanqu").param("exclude", "1"))
                .andExpect(status().isNotFound());
        for (String path : new String[] {"/public/poetry", "/public/poetry/random"})
            mvc.perform(get(path).param("dataset", "unknown")).andExpect(status().isBadRequest());
        poems.deleteById(1);
        poems.deleteById(2);
        mvc.perform(get("/public/poetry/catalog"))
                .andExpect(jsonPath("$.data.collections").isEmpty())
                .andExpect(jsonPath("$.data.dynasties").isEmpty())
                .andExpect(jsonPath("$.data.kinds").isEmpty());
    }
}
