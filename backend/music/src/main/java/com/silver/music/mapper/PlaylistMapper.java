package com.silver.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.silver.music.entity.Playlist;
import com.silver.music.vo.PlaylistVo;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PlaylistMapper extends BaseMapper<Playlist> {
    @Select("SELECT * FROM music_playlist WHERE id = #{id} FOR UPDATE")
    Playlist lock(Long id);

    @Select(
            """
            <script>
            SELECT style
                    FROM music_playlist
                    WHERE id IN
                    <foreach item="id" collection="favoritePlaylistIds" open="(" separator="," close=")">
                        #{id}
                    </foreach>
            </script>
            """)
    List<String> getFavoritePlaylistStyles(
            @Param("favoritePlaylistIds") List<Long> favoritePlaylistIds);

    @Select(
            """
            <script>
            SELECT id AS playlistId, title, cover_url AS coverUrl
                    FROM music_playlist
                    WHERE style IN
                    <foreach item="style" collection="styles" open="(" separator="," close=")">
                        #{style}
                    </foreach>
                    AND id NOT IN
                    <foreach item="id" collection="favoritePlaylistIds" open="(" separator="," close=")">
                        #{id}
                    </foreach>
                    ORDER BY RAND()
                    LIMIT #{limit}
            </script>
            """)
    List<PlaylistVo> getRecommendedPlaylistsByStyles(
            @Param("styles") List<String> styles,
            @Param("favoritePlaylistIds") List<Long> favoritePlaylistIds,
            @Param("limit") int limit);

    @Select(
            """
            SELECT
                p.id AS playlistId,
                p.title AS title,
                p.cover_url AS coverUrl
            FROM music_playlist p
            ORDER BY RAND()
            LIMIT #{limit}
            """)
    List<PlaylistVo> getRandomPlaylists(int limit);

    @Select(
            """
            <script>
            SELECT
                    p.id AS playlistId,
                    p.title AS title,
                    p.cover_url AS coverUrl
                    FROM music_playlist p
                    LEFT JOIN music_user_favorite u ON p.id = u.playlist_id AND u.user_id = #{userId}
                    WHERE
                    <if test="playlistIds != null and playlistIds.size() &gt; 0">
                        p.id IN
                        <foreach collection="playlistIds" item="id" open="(" separator="," close=")">
                            #{id}
                        </foreach>
                    </if>
                    <if test="title != null and title.trim() != ''">
                        AND p.title LIKE CONCAT('%', #{title}, '%')
                    </if>
                    <if test="style != null and style.trim() != ''">
                        AND p.style LIKE CONCAT('%', #{style}, '%')
                    </if>
                    ORDER BY u.create_time DESC
            </script>
            """)
    IPage<PlaylistVo> getPlaylistsByIds(
            @Param("userId") Long userId,
            Page<PlaylistVo> page,
            @Param("playlistIds") List<Long> playlistIds,
            @Param("title") String title,
            @Param("style") String style);
}
