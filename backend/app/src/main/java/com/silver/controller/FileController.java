package com.silver.controller;

import com.silver.account.utils.SecurityUtil;
import com.silver.common.exception.BusinessException;
import com.silver.diary.entity.Article;
import com.silver.diary.service.ArticleService;
import com.silver.storage.service.FileStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class FileController {
    @Autowired private FileStorage storage;

    @Value("${file.access-url-prefix}")
    private String prefix;

    @Autowired private ArticleService articleService;

    @GetMapping("${file.access-url-prefix}{folder}/{name}")
    public ResponseEntity<Resource> file(@PathVariable String folder, @PathVariable String name)
            throws java.io.IOException {
        if (!java.util.Set.of("avatars", "covers").contains(folder)
                || !name.matches("[0-9a-fA-F-]{36}\\.(png|jpg|jpeg|webp)"))
            throw new BusinessException(404, "文件不存在");
        if ("covers".equals(folder)) {
            Article article =
                    articleService
                            .lambdaQuery()
                            .eq(Article::getCoverImg, prefix + folder + "/" + name)
                            .one();
            Long uid = SecurityUtil.optionalUserId();
            if (article == null
                    || (!"公开".equals(article.getState())
                            && !Long.valueOf(article.getCreateUser()).equals(uid))) {
                throw new BusinessException(404, "文件不存在");
            }
        }
        try (var object = storage.read(prefix + folder + "/" + name)) {
            String type =
                    switch (name.substring(name.lastIndexOf('.') + 1)) {
                        case "png" -> "image/png";
                        case "webp" -> "image/webp";
                        default -> "image/jpeg";
                    };
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .contentType(MediaType.parseMediaType(type))
                    .body(new ByteArrayResource(object.readAllBytes()));
        }
    }
}
