package com.silver.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.silver.music.entity.Song;
import com.silver.music.vo.SongAdminVo;
import com.silver.music.vo.SongDetailVo;
import com.silver.music.vo.SongVo;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SongMapper extends BaseMapper<Song> {
    @Select("SELECT * FROM music_song WHERE id = #{id} FOR UPDATE")
    Song lock(Long id);

    @Select(
            """
            SELECT /*+ SET_VAR(group_concat_max_len=16384) */
                s.id AS songId,
                s.name AS songName,
                s.album,
                s.duration,
                s.cover_url AS coverUrl,
                s.audio_url AS audioUrl,
                s.release_time AS releaseTime,
                COALESCE((SELECT GROUP_CONCAT(ma.name ORDER BY sa.position SEPARATOR ' / ') FROM music_song_artist sa JOIN music_artist ma ON ma.id=sa.artist_id WHERE sa.song_id=s.id), a.name) AS artistName
            FROM music_song s
            LEFT JOIN music_artist a ON s.artist_id = a.id
            WHERE
                (#{songName} IS NULL OR s.name LIKE CONCAT('%', #{songName}, '%'))
                AND (#{artistName} IS NULL OR (a.name LIKE CONCAT('%', #{artistName}, '%') OR EXISTS (SELECT 1 FROM music_song_artist sa JOIN music_artist ma ON ma.id=sa.artist_id WHERE sa.song_id=s.id AND ma.name LIKE CONCAT('%', #{artistName}, '%'))))
                AND (#{album} IS NULL OR s.album LIKE CONCAT('%', #{album}, '%'))
            """)
    IPage<SongVo> getSongsWithArtist(
            Page<SongVo> page,
            @Param("songName") String songName,
            @Param("artistName") String artistName,
            @Param("album") String album);

    @Select(
            """
            SELECT /*+ SET_VAR(group_concat_max_len=16384) */
                s.id AS songId,
                s.name AS songName,
                s.artist_id AS artistId,
                s.album,
                s.lyric, s.lyric_url AS lyricUrl,
                s.duration,
                s.style,
                s.cover_url AS coverUrl,
                s.audio_url AS audioUrl,
                s.release_time AS releaseTime,
                COALESCE((SELECT GROUP_CONCAT(ma.name ORDER BY sa.position SEPARATOR ' / ') FROM music_song_artist sa JOIN music_artist ma ON ma.id=sa.artist_id WHERE sa.song_id=s.id), a.name) AS artistName
            FROM music_song s
            LEFT JOIN music_artist a ON s.artist_id = a.id
            WHERE
                (#{artistId} IS NULL OR (s.artist_id = #{artistId} OR EXISTS (SELECT 1 FROM music_song_artist sa WHERE sa.song_id=s.id AND sa.artist_id=#{artistId})))
                AND(#{songName} IS NULL OR s.name LIKE CONCAT('%', #{songName}, '%'))
                AND (#{album} IS NULL OR s.album LIKE CONCAT('%', #{album}, '%'))
            ORDER BY s.release_time DESC
            """)
    IPage<SongAdminVo> getSongsWithArtistName(
            Page<SongAdminVo> page,
            @Param("artistId") Long artistId,
            @Param("songName") String songName,
            @Param("album") String album);

    @Select(
            """
            SELECT /*+ SET_VAR(group_concat_max_len=16384) */
                s.id AS songId,
                s.name AS songName,
                s.album,
                s.duration,
                s.cover_url AS coverUrl,
                s.audio_url AS audioUrl,
                s.release_time AS releaseTime,
                COALESCE((SELECT GROUP_CONCAT(ma.name ORDER BY sa.position SEPARATOR ' / ') FROM music_song_artist sa JOIN music_artist ma ON ma.id=sa.artist_id WHERE sa.song_id=s.id), a.name) AS artistName
            FROM music_song s
            LEFT JOIN music_artist a ON s.artist_id = a.id
            ORDER BY RAND() LIMIT 20
            """)
    List<SongVo> getRandomSongsWithArtist();

    @Select(
            """
            SELECT /*+ SET_VAR(group_concat_max_len=16384) */ s.id AS songId, s.name AS songName, s.album, s.lyric, s.lyric_url AS lyricUrl, s.duration,
                   s.cover_url AS coverUrl, s.audio_url AS audioUrl, s.release_time AS releaseTime,
                   COALESCE((SELECT GROUP_CONCAT(ma.name ORDER BY sa.position SEPARATOR ' / ') FROM music_song_artist sa JOIN music_artist ma ON ma.id=sa.artist_id WHERE sa.song_id=s.id), a.name) AS artistName
            FROM music_song s LEFT JOIN music_artist a ON s.artist_id = a.id
            WHERE s.id = #{songId}
            """)
    SongDetailVo getSongDetailById(Long songId);

    @Select(
            """
            SELECT /*+ SET_VAR(group_concat_max_len=16384) */ s.id AS songId, s.name AS songName, s.album, s.duration,
                   s.cover_url AS coverUrl, s.audio_url AS audioUrl, s.release_time AS releaseTime,
                   COALESCE((SELECT GROUP_CONCAT(ma.name ORDER BY sa.position SEPARATOR ' / ') FROM music_song_artist sa JOIN music_artist ma ON ma.id=sa.artist_id WHERE sa.song_id=s.id), a.name) AS artistName
            FROM music_song s LEFT JOIN music_artist a ON s.artist_id = a.id
            WHERE (s.artist_id = #{artistId} OR EXISTS (SELECT 1 FROM music_song_artist sa WHERE sa.song_id=s.id AND sa.artist_id=#{artistId})) ORDER BY s.id
            """)
    List<SongVo> listByArtist(Long artistId);

    @Select(
            """
            SELECT /*+ SET_VAR(group_concat_max_len=16384) */ s.id AS songId, s.name AS songName, s.album, s.duration,
                   s.cover_url AS coverUrl, s.audio_url AS audioUrl, s.release_time AS releaseTime,
                   COALESCE((SELECT GROUP_CONCAT(ma.name ORDER BY sa.position SEPARATOR ' / ') FROM music_song_artist sa JOIN music_artist ma ON ma.id=sa.artist_id WHERE sa.song_id=s.id), a.name) AS artistName
            FROM music_playlist_binding b JOIN music_song s ON b.song_id = s.id
            LEFT JOIN music_artist a ON s.artist_id = a.id
            WHERE b.playlist_id = #{playlistId} ORDER BY s.id
            """)
    List<SongVo> listByPlaylist(Long playlistId);

    @Select(
            """
            <script>
            SELECT /*+ SET_VAR(group_concat_max_len=16384) */
                    s.id AS songId,
                    s.name AS songName,
                    s.album,
                    s.duration,
                    s.cover_url AS coverUrl,
                    s.audio_url AS audioUrl,
                    s.release_time AS releaseTime,
                    COALESCE((SELECT GROUP_CONCAT(ma.name ORDER BY sa.position SEPARATOR ' / ') FROM music_song_artist sa JOIN music_artist ma ON ma.id=sa.artist_id WHERE sa.song_id=s.id), a.name) AS artistName
                    FROM music_song s
                    LEFT JOIN music_artist a ON s.artist_id = a.id

                    WHERE
                    <if test="songIds != null and songIds.size() &gt; 0">
                        s.id IN
                        <foreach collection="songIds" item="id" open="(" separator="," close=")">
                            #{id}
                        </foreach>
                    </if>
                    <if test="songName != null and songName.trim() != ''">
                        AND s.name LIKE CONCAT('%', #{songName}, '%')
                    </if>
                    <if test="artistName != null and artistName.trim() != ''">
                        AND (a.name LIKE CONCAT('%', #{artistName}, '%') OR EXISTS (SELECT 1 FROM music_song_artist sa JOIN music_artist ma ON ma.id=sa.artist_id WHERE sa.song_id=s.id AND ma.name LIKE CONCAT('%', #{artistName}, '%')))
                    </if>
                    <if test="album != null and album.trim() != ''">
                        AND s.album LIKE CONCAT('%', #{album}, '%')
                    </if>
                    ORDER BY (SELECT f.create_time FROM music_user_favorite f WHERE f.song_id = s.id AND f.user_id = #{userId}) DESC
            </script>
            """)
    IPage<SongVo> getSongsByIds(
            @Param("userId") Long userId,
            Page<SongVo> page,
            @Param("songIds") List<Long> songIds,
            @Param("songName") String songName,
            @Param("artistName") String artistName,
            @Param("album") String album);

    @Select(
            """
            <script>
            SELECT g.style_id
                    FROM music_genre g
                    WHERE g.song_id IN
                    <foreach item="item" collection="favoriteSongIds" open="(" separator="," close=")">
                        #{item}
                    </foreach>
            </script>
            """)
    List<Long> getFavoriteSongStyles(@Param("favoriteSongIds") List<Long> favoriteSongIds);

    @Select(
            """
            <script>
            SELECT /*+ SET_VAR(group_concat_max_len=16384) */ DISTINCT s.id AS songId, s.name AS songName, s.album,
                    s.duration, s.cover_url AS coverUrl, s.audio_url AS audioUrl,
                    s.release_time AS releaseTime, COALESCE((SELECT GROUP_CONCAT(ma.name ORDER BY sa.position SEPARATOR ' / ') FROM music_song_artist sa JOIN music_artist ma ON ma.id=sa.artist_id WHERE sa.song_id=s.id), a.name) AS artistName
                    FROM music_song s
                    JOIN music_genre g ON s.id = g.song_id
                    JOIN music_style st ON g.style_id = st.id
                    LEFT JOIN music_artist a ON s.artist_id = a.id
                    WHERE g.style_id IN
                    <foreach item="item" collection="sortedStyleIds" open="(" separator="," close=")">
                        #{item}
                    </foreach>
                    AND s.id NOT IN
                    <foreach item="item" collection="favoriteSongIds" open="(" separator="," close=")">
                        #{item}
                    </foreach>
                    ORDER BY s.release_time DESC
                    LIMIT #{limit}
            </script>
            """)
    List<SongVo> getRecommendedSongsByStyles(
            @Param("sortedStyleIds") List<Long> sortedStyleIds,
            @Param("favoriteSongIds") List<Long> favoriteSongIds,
            @Param("limit") int limit);
}
