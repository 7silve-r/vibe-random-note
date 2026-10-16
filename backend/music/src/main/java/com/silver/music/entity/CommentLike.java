package com.silver.music.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("music_comment_like")
public class CommentLike {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long commentId;
    private Integer userId;
}
