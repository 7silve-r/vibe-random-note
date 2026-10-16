package com.silver.music.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("music_playlist_binding")
public class PlaylistBinding {

    @TableId(value = "playlist_id", type = IdType.INPUT)
    private Long playlistId;

    private Long songId;
}
