package com.silver.diary.upload;

import com.silver.common.exception.BusinessException;
import com.silver.diary.entity.Article;
import com.silver.diary.service.ArticleService;
import com.silver.storage.model.UploadResult;
import com.silver.storage.model.UploadTarget;
import com.silver.storage.service.FileUploadService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DiaryUploadService {
    private final FileUploadService uploads;
    private final ArticleService articleService;

    public DiaryUploadService(FileUploadService uploads, ArticleService articleService) {
        this.uploads = uploads;
        this.articleService = articleService;
    }

    public UploadResult article(Article article, MultipartFile file) {
        return uploads.replace(
                file,
                UploadTarget.ARTICLE_COVER,
                article.getCoverImg(),
                url -> {
                    article.setCoverImg(url);
                    boolean ok =
                            article.getId() == null
                                    ? articleService.save(article)
                                    : articleService.updateById(article);
                    if (!ok) throw new BusinessException("日记保存失败，请刷新后重试");
                });
    }
}
