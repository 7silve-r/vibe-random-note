package com.silver.music.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("music_style")
public class Style {

    @TableId(value = "id", type = IdType.AUTO)
    private Long styleId;

    private String name;
}
