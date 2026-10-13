package com.silver.diary.service.impl;

import com.silver.account.service.UserDeletionGuard;
import com.silver.common.exception.BusinessException;
import com.silver.diary.entity.Article;
import com.silver.diary.service.ArticleService;
import org.springframework.stereotype.Component;

@Component
public class DiaryUserDeletionGuard implements UserDeletionGuard {
    private final ArticleService articles;

    public DiaryUserDeletionGuard(ArticleService articles) {
        this.articles = articles;
    }

    @Override
    public void check(Integer userId) {
        if (articles
                .lambdaQuery()
                .eq(Article::getCreateUser, userId)
                .last("FOR UPDATE")
                .list()
                .stream()
                .anyMatch(article -> "私有".equals(article.getState()))) {
            throw new BusinessException(403, "该账号存在私有日记，只能停用，不能删除");
        }
    }
}
