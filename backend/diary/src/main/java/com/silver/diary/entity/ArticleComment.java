package com.silver.diary.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("diary_article_comment")
public class ArticleComment {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer articleId;
    private Integer userId;
    private String content;
    private java.time.LocalDateTime createTime;
}
