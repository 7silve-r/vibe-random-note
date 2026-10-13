package com.silver.diary.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.silver.common.exception.BusinessException;
import com.silver.diary.entity.*;
import com.silver.diary.service.*;
import com.silver.diary.support.TestData;
import org.junit.jupiter.api.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class CategoryControllerTest {
    CategoryService categories;
    ArticleService articles;
    CategoryController controller;
    LambdaQueryChainWrapper<Article> query;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        TestData.authenticate(1);
        categories = mock(CategoryService.class);
        articles = mock(ArticleService.class);
        query = mock(LambdaQueryChainWrapper.class);
        doReturn(query).when(query).eq(any(), any());
        when(articles.lambdaQuery()).thenReturn(query);
        controller = new CategoryController();
        ReflectionTestUtils.setField(controller, "categoryService", categories);
        ReflectionTestUtils.setField(controller, "articleService", articles);
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    Category category(int owner) {
        var c = new Category();
        c.setId(7);
        c.setCreateUser(owner);
        c.setCateName("生活");
        c.setCateAlias("life");
        return c;
    }

    @Test
    void addOwner() {
        var c = category(99);
        when(categories.save(any(Category.class))).thenReturn(true);
        controller.add(c);
        assertNull(c.getId());
        assertEquals(1, c.getCreateUser());
    }

    @Test
    void badName() {
        var c = category(1);
        c.setCateName(" ");
        assertThrows(BusinessException.class, () -> controller.add(c));
        verifyNoInteractions(categories);
    }

    @Test
    void inUse() {
        when(categories.getById(7)).thenReturn(category(1));
        when(query.exists()).thenReturn(true);
        assertThrows(BusinessException.class, () -> controller.delete(7));
        verify(categories, never()).removeById(anyInt());
    }

    @Test
    void foreignEdit() {
        when(categories.getById(7)).thenReturn(category(2));
        assertThrows(BusinessException.class, () -> controller.update(category(1)));
        verify(categories, never()).updateById(any(Category.class));
    }

    @Test
    void keepOwner() {
        var saved = category(1);
        when(categories.getById(7)).thenReturn(saved);
        when(categories.updateById(any(Category.class))).thenReturn(true);
        controller.update(category(99));
        assertEquals(1, saved.getCreateUser());
    }

    @Test
    void deleteOk() {
        when(categories.getById(7)).thenReturn(category(1));
        when(categories.removeById(7)).thenReturn(true);
        assertEquals(200, controller.delete(7).getCode());
    }
}
