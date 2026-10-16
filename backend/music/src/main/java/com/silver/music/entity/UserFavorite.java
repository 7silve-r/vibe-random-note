package com.silver.music.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("music_user_favorite")
public class UserFavorite {

    @TableId(value = "id", type = IdType.AUTO)
    private Long userFavoriteId;

    private Long userId;

    private Integer type;

    private Long songId;

    private Long playlistId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
