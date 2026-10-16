package com.silver.diary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("diary_article")
public class Article {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String title;
    private Integer cateId;
    private String coverImg;
    private String content;
    private String state;
    private Integer createUser;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
