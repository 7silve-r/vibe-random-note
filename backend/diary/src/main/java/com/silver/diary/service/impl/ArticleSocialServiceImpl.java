package com.silver.diary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.silver.account.entity.User;
import com.silver.account.service.UserService;
import com.silver.account.utils.SecurityUtil;
import com.silver.common.PageResult;
import com.silver.common.exception.BusinessException;
import com.silver.diary.entity.*;
import com.silver.diary.mapper.*;
import com.silver.diary.service.*;
import com.silver.diary.vo.*;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArticleSocialServiceImpl implements ArticleSocialService {
    @Autowired private ArticleMapper articleMapper;
    @Autowired private ArticleLikeMapper likeMapper;
    @Autowired private ArticleFavoriteMapper favoriteMapper;
    @Autowired private ArticleCommentMapper commentMapper;
    @Autowired private ArticleCommentLikeMapper commentLikeMapper;
    @Autowired private UserService userService;

    private Article visible(Integer id, boolean lock) {
        Article article = lock ? articleMapper.lock(id) : articleMapper.selectById(id);
        if (article == null || !"公开".equals(article.getState())) {
            throw new BusinessException(404, "日记不存在或未公开");
        }
        return article;
    }

    private Integer viewer() {
        try {
            return SecurityUtil.userId();
        } catch (BusinessException ex) {
            return null;
        }
    }

    private ArticleVo view(Article article) {
        ArticleVo vo = new ArticleVo(article);
        User author = userService.getById(article.getCreateUser());
        if (author != null) {
            vo.setAuthorNickname(
                    author.getNickname() == null ? author.getUsername() : author.getNickname());
            vo.setAuthorAvatar(author.getUserPic());
        }
        vo.setLikeCount(
                likeMapper.selectCount(
                        new LambdaQueryWrapper<ArticleLike>()
                                .eq(ArticleLike::getArticleId, article.getId())));
        vo.setFavoriteCount(
                favoriteMapper.selectCount(
                        new LambdaQueryWrapper<ArticleFavorite>()
                                .eq(ArticleFavorite::getArticleId, article.getId())));
        vo.setCommentCount(
                commentMapper.selectCount(
                        new LambdaQueryWrapper<ArticleComment>()
                                .eq(ArticleComment::getArticleId, article.getId())));
        Integer uid = viewer();
        if (uid != null) {
            vo.setLiked(
                    likeMapper.exists(
                            new LambdaQueryWrapper<ArticleLike>()
                                    .eq(ArticleLike::getArticleId, article.getId())
                                    .eq(ArticleLike::getUserId, uid)));
            vo.setCollected(
                    favoriteMapper.exists(
                            new LambdaQueryWrapper<ArticleFavorite>()
                                    .eq(ArticleFavorite::getArticleId, article.getId())
                                    .eq(ArticleFavorite::getUserId, uid)));
        }
        return vo;
    }

    @Override
    public PageResult<ArticleVo> list(Integer pageNum, Integer pageSize, boolean favorites) {
        var query =
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getState, "公开")
                        .orderByDesc(Article::getCreateTime, Article::getId);
        if (favorites)
            query.inSql(
                    Article::getId,
                    "SELECT article_id FROM article_favorite WHERE user_id = "
                            + SecurityUtil.userId());
        var page =
                articleMapper.selectPage(
                        new Page<>(Math.max(1, pageNum), Math.min(100, Math.max(1, pageSize))),
                        query);
        return new PageResult<>(
                page.getTotal(), page.getRecords().stream().map(this::view).toList());
    }

    @Override
    public ArticleVo detail(Integer id) {
        return view(visible(id, false));
    }

    @Override
    @Transactional
    public void like(Integer id, boolean cancel) {
        visible(id, true);
        var query =
                new LambdaQueryWrapper<ArticleLike>()
                        .eq(ArticleLike::getArticleId, id)
                        .eq(ArticleLike::getUserId, SecurityUtil.userId());
        if (cancel) {
            likeMapper.delete(query);
            return;
        }
        if (!likeMapper.exists(query)) {
            ArticleLike like = new ArticleLike();
            like.setArticleId(id);
            like.setUserId(SecurityUtil.userId());
            likeMapper.insert(like);
        }
    }

    @Override
    @Transactional
    public void collect(Integer id, boolean cancel) {
        visible(id, true);
        var query =
                new LambdaQueryWrapper<ArticleFavorite>()
                        .eq(ArticleFavorite::getArticleId, id)
                        .eq(ArticleFavorite::getUserId, SecurityUtil.userId());
        if (cancel) {
            favoriteMapper.delete(query);
            return;
        }
        if (!favoriteMapper.exists(query)) {
            ArticleFavorite favorite = new ArticleFavorite();
            favorite.setArticleId(id);
            favorite.setUserId(SecurityUtil.userId());
            favoriteMapper.insert(favorite);
        }
    }

    @Override
    @Transactional
    public void comment(Integer id, String content) {
        visible(id, true);
        if (content == null || content.isBlank() || content.length() > 255)
            throw new BusinessException("评论须为1到255字");
        ArticleComment comment = new ArticleComment();
        comment.setArticleId(id);
        comment.setUserId(SecurityUtil.userId());
        comment.setContent(content);
        comment.setCreateTime(LocalDateTime.now());
        commentMapper.insert(comment);
    }

    @Override
    public PageResult<ArticleCommentVo> comments(Integer id, Integer pageNum, Integer pageSize) {
        visible(id, false);
        var page =
                commentMapper.selectPage(
                        new Page<>(Math.max(1, pageNum), Math.min(100, Math.max(1, pageSize))),
                        new LambdaQueryWrapper<ArticleComment>()
                                .eq(ArticleComment::getArticleId, id)
                                .orderByDesc(ArticleComment::getId));
        return new PageResult<>(
                page.getTotal(),
                page.getRecords().stream()
                        .map(
                                comment -> {
                                    ArticleCommentVo vo = new ArticleCommentVo();
                                    BeanUtils.copyProperties(comment, vo);
                                    User user = userService.getById(comment.getUserId());
                                    if (user != null) {
                                        vo.setNickname(
                                                user.getNickname() == null
                                                        ? user.getUsername()
                                                        : user.getNickname());
                                        vo.setAvatar(user.getUserPic());
                                    }
                                    vo.setLikeCount(
                                            commentLikeMapper.selectCount(
                                                    new LambdaQueryWrapper<ArticleCommentLike>()
                                                            .eq(
                                                                    ArticleCommentLike
                                                                            ::getCommentId,
                                                                    comment.getId())));
                                    Integer uid = viewer();
                                    vo.setLiked(
                                            uid != null
                                                    && commentLikeMapper.exists(
                                                            new LambdaQueryWrapper<
                                                                            ArticleCommentLike>()
                                                                    .eq(
                                                                            ArticleCommentLike
                                                                                    ::getCommentId,
                                                                            comment.getId())
                                                                    .eq(
                                                                            ArticleCommentLike
                                                                                    ::getUserId,
                                                                            uid)));
                                    return vo;
                                })
                        .toList());
    }

    private ArticleComment comment(Integer id) {
        ArticleComment comment = commentMapper.selectById(id);
        if (comment == null) throw new BusinessException(404, "评论不存在");
        visible(comment.getArticleId(), true);
        return comment;
    }

    @Override
    @Transactional
    public void likeComment(Integer id, boolean cancel) {
        comment(id);
        var query =
                new LambdaQueryWrapper<ArticleCommentLike>()
                        .eq(ArticleCommentLike::getCommentId, id)
                        .eq(ArticleCommentLike::getUserId, SecurityUtil.userId());
        if (cancel) {
            commentLikeMapper.delete(query);
            return;
        }
        if (!commentLikeMapper.exists(query)) {
            ArticleCommentLike like = new ArticleCommentLike();
            like.setCommentId(id);
            like.setUserId(SecurityUtil.userId());
            commentLikeMapper.insert(like);
        }
    }

    @Override
    @Transactional
    public void deleteComment(Integer id) {
        ArticleComment comment = comment(id);
        if (!Objects.equals(comment.getUserId(), SecurityUtil.userId())
                && !SecurityUtil.isAdmin()) {
            throw new BusinessException(403, "只能删除自己的评论");
        }
        commentMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        visible(id, true);
        if (!SecurityUtil.isAdmin()) throw new BusinessException(403, "没有权限执行此操作");
        articleMapper.deleteById(id);
    }
}
