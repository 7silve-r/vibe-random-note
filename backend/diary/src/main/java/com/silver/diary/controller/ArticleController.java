package com.silver.diary.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.silver.account.entity.User;
import com.silver.account.service.UserService;
import com.silver.account.utils.SecurityUtil;
import com.silver.common.PageResult;
import com.silver.common.Result;
import com.silver.common.exception.BusinessException;
import com.silver.diary.dto.ArticleDto;
import com.silver.diary.entity.Article;
import com.silver.diary.entity.Category;
import com.silver.diary.service.ArticleService;
import com.silver.diary.service.CategoryService;
import com.silver.diary.upload.DiaryUploadService;
import com.silver.diary.vo.ArticleVo;
import com.silver.storage.model.UploadResult;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@PreAuthorize("hasRole('USER')")
@RestController
@RequestMapping("/my")
public class ArticleController {
    @Autowired private ArticleService articleService;
    @Autowired private UserService userService;
    @Autowired private CategoryService categoryService;
    @Autowired private DiaryUploadService uploadService;

    @GetMapping("/article/list")
    public Result<PageResult<ArticleVo>> list(
            @RequestParam Integer pageNum,
            @RequestParam Integer pageSize,
            @RequestParam(required = false) Integer cateId,
            @RequestParam(required = false) String state) {
        Integer currentUserId = SecurityUtil.userId();

        Page<Article> page = new Page<>(Math.max(1, pageNum), Math.min(100, Math.max(1, pageSize)));
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        if (cateId != null) wrapper.eq(Article::getCateId, cateId);
        if (state != null && !state.isEmpty()) wrapper.eq(Article::getState, state);

        wrapper.eq(Article::getCreateUser, currentUserId);
        wrapper.orderByDesc(Article::getCreateTime);

        Page<Article> result = articleService.page(page, wrapper);
        List<Integer> userIds =
                result.getRecords().stream()
                        .map(Article::getCreateUser)
                        .distinct()
                        .collect(Collectors.toList());

        Map<Integer, User> userMap;
        if (!userIds.isEmpty()) {
            List<User> users = userService.listByIds(userIds);
            userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));
        } else {
            userMap = new HashMap<>();
        }

        List<ArticleVo> voList =
                result.getRecords().stream()
                        .map(
                                article -> {
                                    ArticleVo vo = new ArticleVo(article);

                                    Category category =
                                            categoryService.getById(article.getCateId());
                                    vo.setCateName(
                                            category != null ? category.getCateName() : "未知");

                                    User author = userMap.get(article.getCreateUser());
                                    if (author != null) {
                                        vo.setAuthorNickname(
                                                author.getNickname() != null
                                                        ? author.getNickname()
                                                        : author.getUsername());
                                        vo.setAuthorAvatar(author.getUserPic());
                                    }

                                    return vo;
                                })
                        .collect(Collectors.toList());

        return Result.success(new PageResult<>(result.getTotal(), voList));
    }

    @GetMapping("/article/info")
    public Result<ArticleVo> detail(@RequestParam Integer id) {
        Integer userId = SecurityUtil.userId();
        Article article = articleService.getById(id);
        if (article == null || !userId.equals(article.getCreateUser())) {
            throw new BusinessException("日记不存在或无权访问");
        }
        Category category = categoryService.getById(article.getCateId());
        ArticleVo vo = new ArticleVo(article);
        vo.setCateName(category != null ? category.getCateName() : "未知");
        User author = userService.getById(article.getCreateUser());
        if (author != null) {
            vo.setAuthorNickname(
                    author.getNickname() != null ? author.getNickname() : author.getUsername());
            vo.setAuthorAvatar(author.getUserPic());
        }
        return Result.success(vo);
    }

    @PostMapping(value = "/article", consumes = "multipart/form-data")
    public Result<UploadResult> publish(@ModelAttribute ArticleDto dto) throws IOException {
        Integer userId = SecurityUtil.userId();
        validate(dto, userId);
        Article article = new Article();
        article.setTitle(dto.getTitle());
        article.setCateId(dto.getCateId());
        article.setContent(dto.getContent());
        article.setState(dto.getState());

        article.setCreateUser(userId);
        article.setCreateTime(LocalDateTime.now());
        article.setUpdateTime(LocalDateTime.now());
        return Result.success(uploadService.article(article, dto.getFile()));
    }

    @PutMapping(value = "/article/{id}", consumes = "multipart/form-data")
    public Result<UploadResult> update(
            @ModelAttribute ArticleDto dto, @PathVariable("id") Integer id) throws IOException {
        Integer userId = SecurityUtil.userId();
        Article article = articleService.getById(id);
        if (article == null || !userId.equals(article.getCreateUser())) {
            throw new BusinessException("日记不存在或无权访问");
        }
        validate(dto, userId);
        article.setTitle(dto.getTitle());
        article.setCateId(dto.getCateId());
        article.setContent(dto.getContent());
        article.setState(dto.getState());
        article.setUpdateTime(LocalDateTime.now());
        return Result.success(uploadService.article(article, dto.getFile()));
    }

    @DeleteMapping("/article")
    public Result<Void> delete(@RequestParam Integer id) {
        Integer userId = SecurityUtil.userId();
        Article article = articleService.getById(id);
        if (article == null || !userId.equals(article.getCreateUser())) {
            throw new BusinessException("日记不存在或无权访问");
        }
        if (!articleService.removeById(id)) {
            throw new BusinessException("操作未完成，数据可能已变化，请刷新后重试");
        }
        return Result.success();
    }

    private void validate(ArticleDto dto, Integer userId) {
        if (dto.getTitle() == null || dto.getTitle().isBlank() || dto.getTitle().length() > 200)
            throw new BusinessException("标题须为1到200字");
        if (dto.getContent() == null
                || dto.getContent().isBlank()
                || dto.getContent().length() > 200000)
            throw new BusinessException("正文不能为空或超过20万字符");
        if (!"私有".equals(dto.getState()) && !"公开".equals(dto.getState())) {
            throw new BusinessException("状态不合法");
        }
        Category category =
                dto.getCateId() == null ? null : categoryService.getById(dto.getCateId());
        if (category == null || !userId.equals(category.getCreateUser())) {
            throw new BusinessException("请选择自己的分类");
        }
    }
}
