package com.silver.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.silver.music.entity.UserFavorite;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserFavoriteMapper extends BaseMapper<UserFavorite> {

    @Select(
            "SELECT song_id FROM music_user_favorite WHERE user_id = #{userId} AND type = 0 ORDER BY create_time DESC")
    List<Long> listSongIds(@Param("userId") Long userId);

    @Select(
            "SELECT playlist_id FROM music_user_favorite WHERE user_id = #{userId} AND type = 1 ORDER BY create_time DESC")
    List<Long> listPlaylistIds(@Param("userId") Long userId);
}
