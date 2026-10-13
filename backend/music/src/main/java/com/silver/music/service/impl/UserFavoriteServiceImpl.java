package com.silver.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.silver.account.utils.SecurityUtil;
import com.silver.common.PageResult;
import com.silver.common.exception.BusinessException;
import com.silver.music.dto.PlaylistQueryDto;
import com.silver.music.dto.SongQueryDto;
import com.silver.music.entity.UserFavorite;
import com.silver.music.mapper.PlaylistMapper;
import com.silver.music.mapper.SongMapper;
import com.silver.music.mapper.UserFavoriteMapper;
import com.silver.music.service.UserFavoriteService;
import com.silver.music.vo.PlaylistVo;
import com.silver.music.vo.SongVo;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserFavoriteServiceImpl extends ServiceImpl<UserFavoriteMapper, UserFavorite>
        implements UserFavoriteService {

    @Autowired private UserFavoriteMapper userFavoriteMapper;
    @Autowired private SongMapper songMapper;
    @Autowired private PlaylistMapper playlistMapper;

    @Override
    public PageResult<SongVo> getUserFavoriteSongs(SongQueryDto songDto) {
        Long userId = SecurityUtil.userId().longValue();

        List<Long> favoriteSongIds = userFavoriteMapper.listSongIds(userId);
        if (favoriteSongIds.isEmpty()) {
            return new PageResult<>(0L, Collections.emptyList());
        }

        Page<SongVo> page = new Page<>(songDto.getPageNum(), songDto.getPageSize());
        IPage<SongVo> songPage =
                songMapper.getSongsByIds(
                        userId,
                        page,
                        favoriteSongIds,
                        songDto.getSongName(),
                        songDto.getArtistName(),
                        songDto.getAlbum());

        List<SongVo> songVoList =
                songPage.getRecords().stream().peek(songVo -> songVo.setCollected(true)).toList();

        return new PageResult<>(songPage.getTotal(), songVoList);
    }

    @Override
    @Transactional
    public void collectSong(Long songId) {
        if (songMapper.lock(songId) == null) throw new BusinessException(404, "歌曲不存在");
        Long userId = SecurityUtil.userId().longValue();

        LambdaQueryWrapper<UserFavorite> query = new LambdaQueryWrapper<>();
        query.eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getType, 0)
                .eq(UserFavorite::getSongId, songId);
        if (userFavoriteMapper.selectCount(query) > 0) {
            return;
        }

        UserFavorite userFavorite = new UserFavorite();
        userFavorite.setUserId(userId);
        userFavorite.setType(0);
        userFavorite.setSongId(songId);
        userFavorite.setCreateTime(LocalDateTime.now());
        userFavoriteMapper.insert(userFavorite);
    }

    @Override
    public void cancelCollectSong(Long songId) {
        Long userId = SecurityUtil.userId().longValue();

        LambdaQueryWrapper<UserFavorite> query = new LambdaQueryWrapper<>();
        query.eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getType, 0)
                .eq(UserFavorite::getSongId, songId);
        userFavoriteMapper.delete(query);
    }

    @Override
    public PageResult<PlaylistVo> getUserFavoritePlaylists(PlaylistQueryDto playlistDto) {
        Long userId = SecurityUtil.userId().longValue();

        List<Long> favoritePlaylistIds = userFavoriteMapper.listPlaylistIds(userId);
        if (favoritePlaylistIds.isEmpty()) {
            return new PageResult<>(0L, Collections.emptyList());
        }

        Page<PlaylistVo> page = new Page<>(playlistDto.getPageNum(), playlistDto.getPageSize());
        IPage<PlaylistVo> playlistPage =
                playlistMapper.getPlaylistsByIds(
                        userId,
                        page,
                        favoritePlaylistIds,
                        playlistDto.getTitle(),
                        playlistDto.getStyle());

        return new PageResult<>(playlistPage.getTotal(), playlistPage.getRecords());
    }

    @Override
    @Transactional
    public void collectPlaylist(Long playlistId) {
        if (playlistMapper.lock(playlistId) == null) throw new BusinessException(404, "歌单不存在");
        Long userId = SecurityUtil.userId().longValue();

        LambdaQueryWrapper<UserFavorite> query = new LambdaQueryWrapper<>();
        query.eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getType, 1)
                .eq(UserFavorite::getPlaylistId, playlistId);
        if (userFavoriteMapper.selectCount(query) > 0) {
            return;
        }

        UserFavorite userFavorite = new UserFavorite();
        userFavorite.setUserId(userId);
        userFavorite.setType(1);
        userFavorite.setPlaylistId(playlistId);
        userFavorite.setCreateTime(LocalDateTime.now());
        userFavoriteMapper.insert(userFavorite);
    }

    @Override
    public void cancelCollectPlaylist(Long playlistId) {
        Long userId = SecurityUtil.userId().longValue();

        LambdaQueryWrapper<UserFavorite> query = new LambdaQueryWrapper<>();
        query.eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getType, 1)
                .eq(UserFavorite::getPlaylistId, playlistId);
        userFavoriteMapper.delete(query);
    }
}
