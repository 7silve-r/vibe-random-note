package com.silver.diary.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.silver.common.exception.BusinessException;
import com.silver.diary.dto.ArticleDto;
import com.silver.diary.entity.*;
import com.silver.diary.service.*;
import com.silver.diary.support.TestData;
import com.silver.diary.upload.*;
import com.silver.storage.model.UploadResult;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class ArticleControllerTest {
    ArticleService articles;
    CategoryService categories;
    DiaryUploadService uploads;
    ArticleController controller;

    @BeforeEach
    void setup() {
        TestData.authenticate(1);
        articles = mock(ArticleService.class);
        categories = mock(CategoryService.class);
        uploads = mock(DiaryUploadService.class);
        controller = new ArticleController();
        ReflectionTestUtils.setField(controller, "articleService", articles);
        ReflectionTestUtils.setField(controller, "categoryService", categories);
        ReflectionTestUtils.setField(controller, "uploadService", uploads);
        var category = new Category();
        category.setId(7);
        category.setCreateUser(1);
        when(categories.getById(7)).thenReturn(category);
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    ArticleDto draft() {
        var dto = new ArticleDto();
        dto.setTitle("今天");
        dto.setContent("正文");
        dto.setCateId(7);
        dto.setState("私有");
        return dto;
    }

    Article stored(int owner) {
        var article = new Article();
        article.setId(10);
        article.setCreateUser(owner);
        article.setCoverImg("/uploads/covers/old.png");
        when(articles.getById(10)).thenReturn(article);
        return article;
    }

    @Test
    void publish() throws Exception {
        when(uploads.article(any(Article.class), isNull())).thenReturn(new UploadResult(null));
        assertEquals(200, controller.publish(draft()).getCode());
        var saved = ArgumentCaptor.forClass(Article.class);
        verify(uploads).article(saved.capture(), isNull());
        assertEquals(1, saved.getValue().getCreateUser());
        assertEquals("今天", saved.getValue().getTitle());
    }

    @Test
    void badTitle() {
        var dto = draft();
        dto.setTitle(" ");
        assertThrows(BusinessException.class, () -> controller.publish(dto));
        verifyNoInteractions(uploads);
    }

    @Test
    void badState() {
        var dto = draft();
        dto.setState("草稿");
        assertThrows(BusinessException.class, () -> controller.publish(dto));
        verifyNoInteractions(uploads);
    }

    @Test
    void foreignCate() {
        var category = new Category();
        category.setId(7);
        category.setCreateUser(2);
        when(categories.getById(7)).thenReturn(category);
        assertThrows(BusinessException.class, () -> controller.publish(draft()));
        verifyNoInteractions(uploads);
    }

    @Test
    void foreignRead() {
        stored(2);
        assertThrows(BusinessException.class, () -> controller.detail(10));
    }

    @Test
    void foreignEdit() {
        stored(2);
        assertThrows(BusinessException.class, () -> controller.update(draft(), 10));
        verifyNoInteractions(uploads);
    }

    @Test
    void foreignDelete() {
        stored(2);
        assertThrows(BusinessException.class, () -> controller.delete(10));
        verify(articles, never()).removeById(anyInt());
    }

    @Test
    void keepCover() throws Exception {
        var article = stored(1);
        controller.update(draft(), 10);
        assertEquals("/uploads/covers/old.png", article.getCoverImg());
        verify(uploads).article(article, null);
    }

    @Test
    void deleteOk() {
        stored(1);
        when(articles.removeById(10)).thenReturn(true);
        assertEquals(200, controller.delete(10).getCode());
    }

    @Test
    void deleteFail() {
        stored(1);
        assertThrows(BusinessException.class, () -> controller.delete(10));
    }
}
