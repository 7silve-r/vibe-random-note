package com.silver.music.mapper;

import com.silver.music.vo.ArtistNameVo;
import java.util.List;
import org.apache.ibatis.annotations.*;

public interface SongArtistMapper {
    @Select(
            "SELECT a.id AS artistId, a.name AS artistName FROM music_song_artist b JOIN music_artist a ON a.id=b.artist_id WHERE b.song_id=#{songId} ORDER BY b.position")
    List<ArtistNameVo> list(Long songId);

    @Select("SELECT id FROM music_artist WHERE id=#{id} FOR UPDATE")
    Long lockArtist(Long id);

    @Delete("DELETE FROM music_song_artist WHERE song_id=#{songId}")
    void delete(Long songId);

    @Insert(
            "INSERT INTO music_song_artist (song_id,artist_id,position) VALUES (#{songId},#{artistId},#{position})")
    void insert(Long songId, Long artistId, int position);

    @Select(
            "SELECT COUNT(*) FROM music_song s WHERE s.artist_id=#{artistId} OR EXISTS (SELECT 1 FROM music_song_artist b WHERE b.song_id=s.id AND b.artist_id=#{artistId})")
    long countSongs(Long artistId);

    @Select("SELECT style_id FROM music_genre WHERE song_id=#{songId} ORDER BY style_id")
    List<Long> styles(Long songId);
}
