package com.silver.poetry.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("poetry_poem")
public class Poem {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String sourceKey;
    private String dataset;
    private String sourceRef;
    private String sourceVersion;
    private Integer authorId;
    private String authorName;
    private String title;
    private String originalTitle;
    private String rhythmic;
    private String dynasty;
    private String kind;
    private String content;
    private String originalContent;
    private String excerpt;
    private Integer featuredRank;
}
