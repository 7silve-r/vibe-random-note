package com.silver.poetry.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("poetry_author")
public class Poet {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String sourceKey;
    private String name;
    private String originalName;
    private String dynasty;
    private String description;
}
