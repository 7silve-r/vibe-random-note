package com.silver.music.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("music_genre")
public class Genre {

    @TableId(value = "song_id", type = IdType.INPUT)
    private Long songId;

    private Long styleId;
}
