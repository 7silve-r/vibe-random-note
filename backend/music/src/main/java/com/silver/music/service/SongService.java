package com.silver.music.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.silver.common.PageResult;
import com.silver.music.dto.AdminSongQueryDto;
import com.silver.music.dto.SongAddDto;
import com.silver.music.dto.SongQueryDto;
import com.silver.music.dto.SongUpdateDto;
import com.silver.music.entity.Song;
import com.silver.music.vo.SongAdminVo;
import com.silver.music.vo.SongDetailVo;
import com.silver.music.vo.SongVo;
import java.util.List;

public interface SongService extends IService<Song> {

    PageResult<SongVo> listSongs(SongQueryDto songDto);

    PageResult<SongAdminVo> listAdminSongs(AdminSongQueryDto songDto);

    List<SongVo> getRecommendedSongs();

    SongDetailVo getSongDetail(Long songId);

    Long countSongs(String style);

    void addSong(SongAddDto songAddDto);

    void updateSong(SongUpdateDto songUpdateDto);

    void updateSongCover(Long songId, String coverUrl);

    void updateSongAudio(Long songId, String audioUrl, String duration);

    void deleteSong(Long songId);

    void deleteSongs(List<Long> songIds);
}
