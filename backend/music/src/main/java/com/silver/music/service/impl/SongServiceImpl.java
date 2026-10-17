package com.silver.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.silver.account.utils.SecurityUtil;
import com.silver.common.PageResult;
import com.silver.common.exception.BusinessException;
import com.silver.music.dto.AdminSongQueryDto;
import com.silver.music.dto.SongAddDto;
import com.silver.music.dto.SongQueryDto;
import com.silver.music.dto.SongUpdateDto;
import com.silver.music.entity.Genre;
import com.silver.music.entity.Song;
import com.silver.music.entity.Style;
import com.silver.music.entity.UserFavorite;
import com.silver.music.mapper.CommentMapper;
import com.silver.music.mapper.GenreMapper;
import com.silver.music.mapper.SongArtistMapper;
import com.silver.music.mapper.SongMapper;
import com.silver.music.mapper.StyleMapper;
import com.silver.music.mapper.UserFavoriteMapper;
import com.silver.music.service.SongService;
import com.silver.music.vo.ArtistNameVo;
import com.silver.music.vo.SongAdminVo;
import com.silver.music.vo.SongDetailVo;
import com.silver.music.vo.SongVo;
import com.silver.storage.service.FileStorage;
import com.silver.storage.support.UploadCleanup;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SongServiceImpl extends ServiceImpl<SongMapper, Song> implements SongService {

    @Autowired private SongMapper songMapper;
    @Autowired private SongArtistMapper songArtists;
    @Autowired private CommentMapper commentMapper;
    @Autowired private UserFavoriteMapper userFavoriteMapper;
    @Autowired private StyleMapper styleMapper;
    @Autowired private GenreMapper genreMapper;
    @Autowired private FileStorage fileStorage;

    @Override
    public PageResult<SongVo> listSongs(SongQueryDto songDto) {

        Long userId = SecurityUtil.optionalUserId();

        Page<SongVo> page = new Page<>(songDto.getPageNum(), songDto.getPageSize());
        IPage<SongVo> songPage =
                songMapper.getSongsWithArtist(
                        page, songDto.getSongName(), songDto.getArtistName(), songDto.getAlbum());

        List<SongVo> songVoList =
                songPage.getRecords().stream().peek(songVo -> songVo.setCollected(false)).toList();

        if (userId != null) {

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

        return new PageResult<>(songPage.getTotal(), songVoList);
    }

    @Override
    public PageResult<SongAdminVo> listAdminSongs(AdminSongQueryDto songDto) {

        Page<SongAdminVo> page = new Page<>(songDto.getPageNum(), songDto.getPageSize());
        IPage<SongAdminVo> songPage =
                songMapper.getSongsWithArtistName(
                        page, songDto.getArtistId(), songDto.getSongName(), songDto.getAlbum());

        songPage.getRecords()
                .forEach(
                        row -> {
                            var artists = songArtists.list(row.getSongId());
                            row.setArtists(artists);
                            row.setArtistIds(
                                    artists.stream().map(ArtistNameVo::getArtistId).toList());
                            row.setStyleIds(songArtists.styles(row.getSongId()));
                        });
        return new PageResult<>(songPage.getTotal(), songPage.getRecords());
    }

    @Override
    public List<SongVo> getRecommendedSongs() {

        Long userId = SecurityUtil.optionalUserId();

        if (userId == null) {
            return songMapper.getRandomSongsWithArtist();
        }

        List<Long> favoriteSongIds = userFavoriteMapper.listSongIds(userId);
        if (favoriteSongIds.isEmpty()) {
            return songMapper.getRandomSongsWithArtist();
        }

        List<Long> favoriteStyleIds = songMapper.getFavoriteSongStyles(favoriteSongIds);
        Map<Long, Long> styleFrequency =
                favoriteStyleIds.stream()
                        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        List<Long> sortedStyleIds =
                styleFrequency.entrySet().stream()
                        .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());

        List<SongVo> candidateSongs =
                sortedStyleIds.isEmpty()
                        ? new ArrayList<>()
                        : new ArrayList<>(
                                songMapper.getRecommendedSongsByStyles(
                                        sortedStyleIds, favoriteSongIds, 80));

        Collections.shuffle(candidateSongs);
        List<SongVo> recommendedSongs =
                candidateSongs.subList(0, Math.min(20, candidateSongs.size()));

        if (recommendedSongs.size() < 20) {
            List<SongVo> randomSongs = songMapper.getRandomSongsWithArtist();
            Set<Long> addedSongIds =
                    recommendedSongs.stream().map(SongVo::getSongId).collect(Collectors.toSet());
            for (SongVo song : randomSongs) {
                if (recommendedSongs.size() >= 20) break;
                if (!addedSongIds.contains(song.getSongId())) {
                    recommendedSongs.add(song);
                    addedSongIds.add(song.getSongId());
                }
            }
        }

        return recommendedSongs;
    }

    @Override
    public SongDetailVo getSongDetail(Long songId) {
        SongDetailVo songDetailVo = songMapper.getSongDetailById(songId);
        if (songDetailVo == null) throw new BusinessException(404, "歌曲不存在");
        songDetailVo.setArtists(songArtists.list(songId));
        songDetailVo.setArtistIds(
                songDetailVo.getArtists().stream().map(ArtistNameVo::getArtistId).toList());
        songDetailVo.setStyleIds(songArtists.styles(songId));
        songDetailVo.setComments(commentMapper.listByTarget(songId, 0));
        songDetailVo.setCollected(false);

        Long userId = SecurityUtil.optionalUserId();

        if (userId != null) {

            UserFavorite favoriteSong =
                    userFavoriteMapper.selectOne(
                            new LambdaQueryWrapper<UserFavorite>()
                                    .eq(UserFavorite::getUserId, userId)
                                    .eq(UserFavorite::getType, 0)
                                    .eq(UserFavorite::getSongId, songId));
            if (favoriteSong != null) {
                songDetailVo.setCollected(true);
            }
        }

        return songDetailVo;
    }

    @Override
    public Long countSongs(String style) {
        LambdaQueryWrapper<Song> query = new LambdaQueryWrapper<>();
        if (style != null) {
            query.like(Song::getStyle, style);
        }

        return songMapper.selectCount(query);
    }

    private List<Long> artistIds(List<Long> ids, Long legacy) {
        List<Long> selected = ids != null ? ids : legacy == null ? List.of() : List.of(legacy);
        if (selected.isEmpty()
                || selected.size() > 20
                || selected.stream().anyMatch(id -> id == null || id <= 0))
            throw new BusinessException("请选择1到20位歌手");
        var distinct = selected.stream().distinct().toList();
        for (Long id : distinct.stream().sorted().toList())
            if (songArtists.lockArtist(id) == null) throw new BusinessException("所选歌手不存在");
        return distinct;
    }

    private List<Style> styles(List<Long> ids, String legacy) {
        if (ids != null) {
            if (ids.size() > 30 || ids.stream().anyMatch(id -> id == null || id <= 0))
                throw new BusinessException("风格选择不正确");
            var distinct = ids.stream().distinct().toList();
            if (distinct.isEmpty()) return List.of();
            var found = styleMapper.selectByIds(distinct);
            if (found.size() != distinct.size()) throw new BusinessException("所选风格不存在");
            return found;
        }
        if (legacy == null || legacy.isBlank()) return List.of();
        var names =
                Arrays.stream(legacy.split(","))
                        .map(String::strip)
                        .filter(s -> !s.isEmpty())
                        .distinct()
                        .toList();
        if (names.isEmpty()) return List.of();
        var found =
                styleMapper.selectList(new LambdaQueryWrapper<Style>().in(Style::getName, names));
        if (found.size() != names.size()) throw new BusinessException("请从已有风格中选择");
        return found;
    }

    private void associations(Long songId, List<Long> artists, List<Style> styles) {
        songArtists.delete(songId);
        for (int i = 0; i < artists.size(); i++) songArtists.insert(songId, artists.get(i), i);
        genreMapper.delete(new LambdaQueryWrapper<Genre>().eq(Genre::getSongId, songId));
        for (Style style : styles) {
            Genre genre = new Genre();
            genre.setSongId(songId);
            genre.setStyleId(style.getStyleId());
            genreMapper.insert(genre);
        }
    }

    private String styleNames(List<Style> styles) {
        String names = styles.stream().map(Style::getName).collect(Collectors.joining(","));
        if (names.length() > 255) throw new BusinessException("所选风格名称总长度不能超过255字");
        return names;
    }

    @Override
    @Transactional
    public void addSong(SongAddDto dto) {
        var artists = artistIds(dto.getArtistIds(), dto.getArtistId());
        var styles = styles(dto.getStyleIds(), dto.getStyle());
        Song song = new Song();
        BeanUtils.copyProperties(dto, song);
        song.setArtistId(artists.get(0));
        song.setStyle(styleNames(styles));
        if (songMapper.insert(song) == 0) throw new BusinessException("添加失败");
        associations(song.getSongId(), artists, styles);
    }

    @Override
    @Transactional
    public void updateSong(SongUpdateDto dto) {
        var artists = artistIds(dto.getArtistIds(), dto.getArtistId());
        var styles = styles(dto.getStyleIds(), dto.getStyle());
        if (songMapper.lock(dto.getSongId()) == null) throw new BusinessException(404, "歌曲不存在");
        Song song = new Song();
        BeanUtils.copyProperties(dto, song);
        song.setArtistId(artists.get(0));
        song.setStyle(styleNames(styles));
        if (songMapper.updateById(song) == 0) throw new BusinessException("更新失败");
        associations(song.getSongId(), artists, styles);
    }

    @Override
    @Transactional
    public void updateSongLyric(Long songId, String url, String lyric) {
        Song song = songMapper.lock(songId);
        if (song == null) throw new BusinessException(404, "歌曲不存在");
        songMapper.update(
                null,
                new LambdaUpdateWrapper<Song>()
                        .eq(Song::getSongId, songId)
                        .set(Song::getLyricUrl, url)
                        .set(Song::getLyric, lyric));
        UploadCleanup.afterCommit(fileStorage, song.getLyricUrl());
    }

    @Override
    @Transactional
    public void updateSongCover(Long songId, String coverUrl) {
        Song song = songMapper.selectById(songId);
        if (song == null) throw new BusinessException(404, "资源不存在");
        String cover = song.getCoverUrl();

        song.setCoverUrl(coverUrl);
        if (songMapper.updateById(song) == 0) {
            throw new BusinessException("更新失败");
        }

        UploadCleanup.afterCommit(fileStorage, cover);
    }

    @Override
    @Transactional
    public void updateSongAudio(Long songId, String audioUrl, String duration) {
        Song song = songMapper.selectById(songId);
        if (song == null) throw new BusinessException(404, "资源不存在");
        String audio = song.getAudioUrl();

        song.setAudioUrl(audioUrl);
        song.setDuration(duration);
        if (songMapper.updateById(song) == 0) {
            throw new BusinessException("更新失败");
        }

        UploadCleanup.afterCommit(fileStorage, audio);
    }

    @Override
    @Transactional
    public void deleteSong(Long songId) {
        Song song = songMapper.selectById(songId);
        if (song == null) {
            throw new BusinessException("歌曲不存在");
        }
        String cover = song.getCoverUrl();
        String audio = song.getAudioUrl();
        UploadCleanup.afterCommit(fileStorage, song.getLyricUrl());

        if (cover != null && !cover.isEmpty()) {
            UploadCleanup.afterCommit(fileStorage, cover);
        }
        if (audio != null && !audio.isEmpty()) {
            UploadCleanup.afterCommit(fileStorage, audio);
        }

        if (songMapper.deleteById(songId) == 0) {
            throw new BusinessException("删除失败");
        }
    }

    @Override
    @Transactional
    public void deleteSongs(List<Long> songIds) {

        List<Song> songs = songMapper.selectByIds(songIds);
        songs.forEach(song -> UploadCleanup.afterCommit(fileStorage, song.getLyricUrl()));
        List<String> coverUrlList =
                songs.stream()
                        .map(Song::getCoverUrl)
                        .filter(coverUrl -> coverUrl != null && !coverUrl.isEmpty())
                        .toList();
        List<String> audioUrlList =
                songs.stream()
                        .map(Song::getAudioUrl)
                        .filter(audioUrl -> audioUrl != null && !audioUrl.isEmpty())
                        .toList();

        for (String coverUrl : coverUrlList) {
            UploadCleanup.afterCommit(fileStorage, coverUrl);
        }
        for (String audioUrl : audioUrlList) {
            UploadCleanup.afterCommit(fileStorage, audioUrl);
        }

        if (songMapper.deleteByIds(songIds) == 0) {
            throw new BusinessException("删除失败");
        }
    }
}
