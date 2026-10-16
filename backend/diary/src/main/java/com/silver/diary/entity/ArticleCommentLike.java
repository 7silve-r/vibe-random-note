package com.silver.diary.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("diary_article_comment_like")
public class ArticleCommentLike {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer commentId;
    private Integer userId;
}
