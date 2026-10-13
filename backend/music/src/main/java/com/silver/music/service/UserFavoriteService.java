package com.silver.music.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.silver.common.PageResult;
import com.silver.music.dto.PlaylistQueryDto;
import com.silver.music.dto.SongQueryDto;
import com.silver.music.entity.UserFavorite;
import com.silver.music.vo.PlaylistVo;
import com.silver.music.vo.SongVo;

public interface UserFavoriteService extends IService<UserFavorite> {

    PageResult<SongVo> getUserFavoriteSongs(SongQueryDto songDto);

    void collectSong(Long songId);

    void cancelCollectSong(Long songId);

    PageResult<PlaylistVo> getUserFavoritePlaylists(PlaylistQueryDto playlistDto);

    void collectPlaylist(Long playlistId);

    void cancelCollectPlaylist(Long playlistId);
}
