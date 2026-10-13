package com.silver.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.silver.account.utils.SecurityUtil;
import com.silver.common.PageResult;
import com.silver.common.exception.BusinessException;
import com.silver.music.dto.PlaylistAddDto;
import com.silver.music.dto.PlaylistQueryDto;
import com.silver.music.dto.PlaylistUpdateDto;
import com.silver.music.entity.Playlist;
import com.silver.music.entity.UserFavorite;
import com.silver.music.mapper.CommentMapper;
import com.silver.music.mapper.PlaylistMapper;
import com.silver.music.mapper.SongMapper;
import com.silver.music.mapper.UserFavoriteMapper;
import com.silver.music.service.PlaylistService;
import com.silver.music.vo.PlaylistDetailVo;
import com.silver.music.vo.PlaylistVo;
import com.silver.music.vo.SongVo;
import com.silver.storage.service.FileStorage;
import com.silver.storage.support.UploadCleanup;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlaylistServiceImpl extends ServiceImpl<PlaylistMapper, Playlist>
        implements PlaylistService {

    @Autowired private PlaylistMapper playlistMapper;
    @Autowired private SongMapper songMapper;
    @Autowired private CommentMapper commentMapper;
    @Autowired private UserFavoriteMapper userFavoriteMapper;
    @Autowired private FileStorage fileStorage;

    @Override
    public PageResult<PlaylistVo> listPlaylists(PlaylistQueryDto playlistDto) {

        Page<Playlist> page = new Page<>(playlistDto.getPageNum(), playlistDto.getPageSize());
        LambdaQueryWrapper<Playlist> query = new LambdaQueryWrapper<>();

        if (playlistDto.getTitle() != null) {
            query.like(Playlist::getTitle, playlistDto.getTitle());
        }
        if (playlistDto.getStyle() != null && !playlistDto.getStyle().isBlank()) {
            query.eq(Playlist::getStyle, playlistDto.getStyle());
        }

        IPage<Playlist> playlistPage = playlistMapper.selectPage(page, query);

        List<PlaylistVo> playlistVoList =
                playlistPage.getRecords().stream()
                        .map(
                                playlist -> {
                                    PlaylistVo playlistVo = new PlaylistVo();
                                    BeanUtils.copyProperties(playlist, playlistVo);
                                    return playlistVo;
                                })
                        .toList();

        return new PageResult<>(playlistPage.getTotal(), playlistVoList);
    }

    @Override
    public PageResult<Playlist> listAdminPlaylists(PlaylistQueryDto playlistDto) {

        Page<Playlist> page = new Page<>(playlistDto.getPageNum(), playlistDto.getPageSize());
        LambdaQueryWrapper<Playlist> query = new LambdaQueryWrapper<>();

        if (playlistDto.getTitle() != null) {
            query.like(Playlist::getTitle, playlistDto.getTitle());
        }
        if (playlistDto.getStyle() != null && !playlistDto.getStyle().isBlank()) {
            query.eq(Playlist::getStyle, playlistDto.getStyle());
        }

        query.orderByDesc(Playlist::getPlaylistId);

        IPage<Playlist> playlistPage = playlistMapper.selectPage(page, query);

        return new PageResult<>(playlistPage.getTotal(), playlistPage.getRecords());
    }

    @Override
    public List<PlaylistVo> getRecommendedPlaylists() {

        Long userId = SecurityUtil.optionalUserId();

        if (userId == null) {
            return playlistMapper.getRandomPlaylists(10);
        }

        List<Long> favoritePlaylistIds = userFavoriteMapper.listPlaylistIds(userId);
        if (favoritePlaylistIds.isEmpty()) {
            return playlistMapper.getRandomPlaylists(10);
        }

        List<String> favoriteStyles = playlistMapper.getFavoritePlaylistStyles(favoritePlaylistIds);
        List<String> styles =
                favoriteStyles.stream()
                        .filter(style -> style != null && !style.isBlank())
                        .distinct()
                        .toList();

        List<PlaylistVo> recommendedPlaylists =
                styles.isEmpty()
                        ? new ArrayList<>()
                        : new ArrayList<>(
                                playlistMapper.getRecommendedPlaylistsByStyles(
                                        styles, favoritePlaylistIds, 10));

        if (recommendedPlaylists.size() < 10) {
            List<PlaylistVo> randomPlaylists = playlistMapper.getRandomPlaylists(10);
            Set<Long> addedPlaylistIds =
                    recommendedPlaylists.stream()
                            .map(PlaylistVo::getPlaylistId)
                            .collect(Collectors.toSet());

            for (PlaylistVo playlist : randomPlaylists) {
                if (recommendedPlaylists.size() >= 10) break;
                if (!addedPlaylistIds.contains(playlist.getPlaylistId())) {
                    recommendedPlaylists.add(playlist);
                    addedPlaylistIds.add(playlist.getPlaylistId());
                }
            }
        }

        return recommendedPlaylists;
    }

    @Override
    public PlaylistDetailVo getPlaylistDetail(Long playlistId) {
        Playlist playlist = playlistMapper.selectById(playlistId);
        if (playlist == null) throw new BusinessException(404, "歌单不存在");

        PlaylistDetailVo playlistDetailVo = new PlaylistDetailVo();
        BeanUtils.copyProperties(playlist, playlistDetailVo);
        List<SongVo> songVoList = songMapper.listByPlaylist(playlistId);
        playlistDetailVo.setSongs(songVoList);
        playlistDetailVo.setComments(commentMapper.listByTarget(playlistId, 1));
        songVoList.forEach(songVo -> songVo.setCollected(false));
        playlistDetailVo.setCollected(false);

        Long userId = SecurityUtil.optionalUserId();

        if (userId != null) {

            UserFavorite favoritePlaylist =
                    userFavoriteMapper.selectOne(
                            new LambdaQueryWrapper<UserFavorite>()
                                    .eq(UserFavorite::getUserId, userId)
                                    .eq(UserFavorite::getType, 1)
                                    .eq(UserFavorite::getPlaylistId, playlistId));
            if (favoritePlaylist != null) {
                playlistDetailVo.setCollected(true);
            }

            List<UserFavorite> favoriteSongs =
                    userFavoriteMapper.selectList(
                            new LambdaQueryWrapper<UserFavorite>()
                                    .eq(UserFavorite::getUserId, userId)
                                    .eq(UserFavorite::getType, 0));

            Set<Long> favoriteSongIds =
                    favoriteSongs.stream().map(UserFavorite::getSongId).collect(Collectors.toSet());

            for (SongVo songVo : songVoList) {
                if (favoriteSongIds.contains(songVo.getSongId())) {
                    songVo.setCollected(true);
                }
            }
        }

        return playlistDetailVo;
    }

    @Override
    public Long countPlaylists(String style) {
        LambdaQueryWrapper<Playlist> query = new LambdaQueryWrapper<>();
        if (style != null && !style.isBlank()) {
            query.eq(Playlist::getStyle, style);
        }

        return playlistMapper.selectCount(query);
    }

    @Override
    @Transactional
    public void addPlaylist(PlaylistAddDto playlistAddDto) {
        LambdaQueryWrapper<Playlist> query = new LambdaQueryWrapper<>();
        query.eq(Playlist::getTitle, playlistAddDto.getTitle());
        if (playlistMapper.selectCount(query) > 0) {
            throw new BusinessException("歌单已存在");
        }

        Playlist playlist = new Playlist();
        BeanUtils.copyProperties(playlistAddDto, playlist);
        playlistMapper.insert(playlist);
    }

    @Override
    @Transactional
    public void updatePlaylist(PlaylistUpdateDto playlistUpdateDto) {
        Long playlistId = playlistUpdateDto.getPlaylistId();

        Playlist playlistByTitle =
                playlistMapper.selectOne(
                        new LambdaQueryWrapper<Playlist>()
                                .eq(Playlist::getTitle, playlistUpdateDto.getTitle()));
        if (playlistByTitle != null && !playlistByTitle.getPlaylistId().equals(playlistId)) {
            throw new BusinessException("歌单已存在");
        }

        Playlist playlist = new Playlist();
        BeanUtils.copyProperties(playlistUpdateDto, playlist);
        if (playlistMapper.updateById(playlist) == 0) {
            throw new BusinessException("更新失败");
        }
    }

    @Override
    @Transactional
    public void updatePlaylistCover(Long playlistId, String coverUrl) {
        Playlist playlist = playlistMapper.selectById(playlistId);
        if (playlist == null) throw new BusinessException(404, "资源不存在");
        String cover = playlist.getCoverUrl();

        playlist.setCoverUrl(coverUrl);
        if (playlistMapper.updateById(playlist) == 0) {
            throw new BusinessException("更新失败");
        }

        UploadCleanup.afterCommit(fileStorage, cover);
    }

    @Override
    @Transactional
    public void deletePlaylist(Long playlistId) {

        Playlist playlist = playlistMapper.selectById(playlistId);
        if (playlist == null) {
            throw new BusinessException("歌单不存在");
        }
        String coverUrl = playlist.getCoverUrl();

        if (coverUrl != null && !coverUrl.isEmpty()) {
            UploadCleanup.afterCommit(fileStorage, coverUrl);
        }

        if (playlistMapper.deleteById(playlistId) == 0) {
            throw new BusinessException("删除失败");
        }
    }

    @Override
    @Transactional
    public void deletePlaylists(List<Long> playlistIds) {
        List<Playlist> playlists = playlistMapper.selectByIds(playlistIds);
        List<String> coverUrlList =
                playlists.stream()
                        .map(Playlist::getCoverUrl)
                        .filter(coverUrl -> coverUrl != null && !coverUrl.isEmpty())
                        .toList();

        for (String coverUrl : coverUrlList) {
            UploadCleanup.afterCommit(fileStorage, coverUrl);
        }

        if (playlistMapper.deleteByIds(playlistIds) == 0) {
            throw new BusinessException("删除失败");
        }
    }
}
