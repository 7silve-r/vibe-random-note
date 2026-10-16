package com.silver.music.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("music_playlist")
public class Playlist {

    @TableId(value = "id", type = IdType.AUTO)
    private Long playlistId;

    private String title;

    private String coverUrl;

    private String introduction;

    private String style;
}
