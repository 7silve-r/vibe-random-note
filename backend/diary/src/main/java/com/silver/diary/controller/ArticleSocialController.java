package com.silver.diary.controller;

import com.silver.common.*;
import com.silver.diary.dto.ArticleCommentDto;
import com.silver.diary.service.ArticleSocialService;
import com.silver.diary.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class ArticleSocialController {
    @Autowired private ArticleSocialService articleSocialService;

    @GetMapping("/public/articles")
    public Result<PageResult<ArticleVo>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(articleSocialService.list(pageNum, pageSize, false));
    }

    @GetMapping("/public/articles/{id}")
    public Result<ArticleVo> detail(@PathVariable Integer id) {
        return Result.success(articleSocialService.detail(id));
    }

    @GetMapping("/my/article/favorites")
    @PreAuthorize("hasRole('USER')")
    public Result<PageResult<ArticleVo>> favorites(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(articleSocialService.list(pageNum, pageSize, true));
    }

    @GetMapping("/public/articles/{id}/comments")
    public Result<PageResult<ArticleCommentVo>> comments(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(articleSocialService.comments(id, pageNum, pageSize));
    }

    @PostMapping("/my/article/{id}/comment")
    @PreAuthorize("hasRole('USER')")
    public Result<Void> comment(@PathVariable Integer id, @RequestBody ArticleCommentDto dto) {
        articleSocialService.comment(id, dto.getContent());
        return Result.success();
    }

    @DeleteMapping("/my/article/comments/{id}")
    @PreAuthorize("hasRole('USER')")
    public Result<Void> deleteComment(@PathVariable Integer id) {
        articleSocialService.deleteComment(id);
        return Result.success();
    }

    @DeleteMapping("/admin/diary/articles/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Integer id) {
        articleSocialService.delete(id);
        return Result.success();
    }

    @PostMapping("/my/article/{id}/like")
    @PreAuthorize("hasRole('USER')")
    public Result<Void> like(@PathVariable Integer id) {
        articleSocialService.like(id, false);
        return Result.success();
    }

    @DeleteMapping("/my/article/{id}/like")
    @PreAuthorize("hasRole('USER')")
    public Result<Void> cancelLike(@PathVariable Integer id) {
        articleSocialService.like(id, true);
        return Result.success();
    }

    @PostMapping("/my/article/{id}/favorite")
    @PreAuthorize("hasRole('USER')")
    public Result<Void> collect(@PathVariable Integer id) {
        articleSocialService.collect(id, false);
        return Result.success();
    }

    @DeleteMapping("/my/article/{id}/favorite")
    @PreAuthorize("hasRole('USER')")
    public Result<Void> cancelCollect(@PathVariable Integer id) {
        articleSocialService.collect(id, true);
        return Result.success();
    }

    @PostMapping("/my/article/comments/{id}/like")
    @PreAuthorize("hasRole('USER')")
    public Result<Void> likeComment(@PathVariable Integer id) {
        articleSocialService.likeComment(id, false);
        return Result.success();
    }

    @DeleteMapping("/my/article/comments/{id}/like")
    @PreAuthorize("hasRole('USER')")
    public Result<Void> cancelLikeComment(@PathVariable Integer id) {
        articleSocialService.likeComment(id, true);
        return Result.success();
    }
}
