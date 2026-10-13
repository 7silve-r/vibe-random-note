package com.silver.music.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.silver.common.PageResult;
import com.silver.music.dto.PlaylistAddDto;
import com.silver.music.dto.PlaylistQueryDto;
import com.silver.music.dto.PlaylistUpdateDto;
import com.silver.music.entity.Playlist;
import com.silver.music.vo.PlaylistDetailVo;
import com.silver.music.vo.PlaylistVo;
import java.util.List;

public interface PlaylistService extends IService<Playlist> {

    PageResult<PlaylistVo> listPlaylists(PlaylistQueryDto playlistDto);

    PageResult<Playlist> listAdminPlaylists(PlaylistQueryDto playlistDto);

    List<PlaylistVo> getRecommendedPlaylists();

    PlaylistDetailVo getPlaylistDetail(Long playlistId);

    Long countPlaylists(String style);

    void addPlaylist(PlaylistAddDto playlistAddDto);

    void updatePlaylist(PlaylistUpdateDto playlistUpdateDto);

    void updatePlaylistCover(Long playlistId, String coverUrl);

    void deletePlaylist(Long playlistId);

    void deletePlaylists(List<Long> playlistIds);
}
