package com.silver.diary.service;

import com.silver.common.PageResult;
import com.silver.diary.vo.ArticleCommentVo;
import com.silver.diary.vo.ArticleVo;

public interface ArticleSocialService {
    PageResult<ArticleVo> list(Integer pageNum, Integer pageSize, boolean favorites);

    ArticleVo detail(Integer id);

    void like(Integer id, boolean cancel);

    void collect(Integer id, boolean cancel);

    void comment(Integer id, String content);

    PageResult<ArticleCommentVo> comments(Integer id, Integer pageNum, Integer pageSize);

    void likeComment(Integer id, boolean cancel);

    void deleteComment(Integer id);

    void delete(Integer id);
}
