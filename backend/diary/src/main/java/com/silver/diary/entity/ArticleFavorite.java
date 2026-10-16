package com.silver.diary.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("diary_article_favorite")
public class ArticleFavorite {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer articleId;
    private Integer userId;
}
