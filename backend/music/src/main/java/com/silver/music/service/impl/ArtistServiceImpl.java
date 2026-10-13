package com.silver.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.silver.account.utils.SecurityUtil;
import com.silver.common.PageResult;
import com.silver.common.exception.BusinessException;
import com.silver.music.dto.ArtistAddDto;
import com.silver.music.dto.ArtistQueryDto;
import com.silver.music.dto.ArtistUpdateDto;
import com.silver.music.entity.Artist;
import com.silver.music.entity.UserFavorite;
import com.silver.music.mapper.ArtistMapper;
import com.silver.music.mapper.SongMapper;
import com.silver.music.mapper.UserFavoriteMapper;
import com.silver.music.service.ArtistService;
import com.silver.music.vo.ArtistDetailVo;
import com.silver.music.vo.ArtistNameVo;
import com.silver.music.vo.ArtistVo;
import com.silver.music.vo.SongVo;
import com.silver.storage.service.FileStorage;
import com.silver.storage.support.UploadCleanup;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArtistServiceImpl extends ServiceImpl<ArtistMapper, Artist> implements ArtistService {

    @Autowired private ArtistMapper artistMapper;
    @Autowired private SongMapper songMapper;
    @Autowired private UserFavoriteMapper userFavoriteMapper;
    @Autowired private FileStorage fileStorage;

    @Override
    public PageResult<ArtistVo> listArtists(ArtistQueryDto artistDto) {

        Page<Artist> page = new Page<>(artistDto.getPageNum(), artistDto.getPageSize());
        LambdaQueryWrapper<Artist> query = new LambdaQueryWrapper<>();

        if (artistDto.getArtistName() != null) {
            query.like(Artist::getArtistName, artistDto.getArtistName());
        }
        if (artistDto.getGender() != null) {
            query.eq(Artist::getGender, artistDto.getGender());
        }
        if (artistDto.getArea() != null) {
            query.like(Artist::getArea, artistDto.getArea());
        }

        IPage<Artist> artistPage = artistMapper.selectPage(page, query);

        List<ArtistVo> artistVoList =
                artistPage.getRecords().stream()
                        .map(
                                artist -> {
                                    ArtistVo artistVo = new ArtistVo();
                                    BeanUtils.copyProperties(artist, artistVo);
                                    return artistVo;
                                })
                        .toList();

        return new PageResult<>(artistPage.getTotal(), artistVoList);
    }

    @Override
    public PageResult<Artist> listAdminArtists(ArtistQueryDto artistDto) {

        Page<Artist> page = new Page<>(artistDto.getPageNum(), artistDto.getPageSize());
        LambdaQueryWrapper<Artist> query = new LambdaQueryWrapper<>();

        if (artistDto.getArtistName() != null) {
            query.like(Artist::getArtistName, artistDto.getArtistName());
        }
        if (artistDto.getGender() != null) {
            query.eq(Artist::getGender, artistDto.getGender());
        }
        if (artistDto.getArea() != null) {
            query.like(Artist::getArea, artistDto.getArea());
        }

        query.orderByDesc(Artist::getArtistId);

        IPage<Artist> artistPage = artistMapper.selectPage(page, query);

        return new PageResult<>(artistPage.getTotal(), artistPage.getRecords());
    }

    @Override
    public List<ArtistNameVo> listArtistNames() {
        List<Artist> artists =
                artistMapper.selectList(
                        new LambdaQueryWrapper<Artist>().orderByDesc(Artist::getArtistId));

        return artists.stream()
                .map(
                        artist -> {
                            ArtistNameVo artistNameVo = new ArtistNameVo();
                            artistNameVo.setArtistId(artist.getArtistId());
                            artistNameVo.setArtistName(artist.getArtistName());
                            return artistNameVo;
                        })
                .toList();
    }

    @Override
    public List<ArtistVo> getRandomArtists() {
        LambdaQueryWrapper<Artist> query = new LambdaQueryWrapper<>();
        query.last("ORDER BY RAND() LIMIT 10");

        List<Artist> artists = artistMapper.selectList(query);

        return artists.stream()
                .map(
                        artist -> {
                            ArtistVo artistVo = new ArtistVo();
                            BeanUtils.copyProperties(artist, artistVo);
                            return artistVo;
                        })
                .toList();
    }

    @Override
    public ArtistDetailVo getArtistDetail(Long artistId) {
        Artist artist = artistMapper.selectById(artistId);
        if (artist == null) throw new BusinessException(404, "歌手不存在");

        ArtistDetailVo artistDetailVo = new ArtistDetailVo();
        BeanUtils.copyProperties(artist, artistDetailVo);
        List<SongVo> songVoList = songMapper.listByArtist(artistId);
        songVoList.forEach(songVo -> songVo.setCollected(false));

        Long userId = SecurityUtil.optionalUserId();

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

        artistDetailVo.setSongs(songVoList);

        return artistDetailVo;
    }

    @Override
    public Long countArtists(Integer gender, String area) {
        LambdaQueryWrapper<Artist> query = new LambdaQueryWrapper<>();
        if (gender != null) {
            query.eq(Artist::getGender, gender);
        }
        if (area != null) {
            query.eq(Artist::getArea, area);
        }

        return artistMapper.selectCount(query);
    }

    @Override
    @Transactional
    public void addArtist(ArtistAddDto artistAddDto) {
        LambdaQueryWrapper<Artist> query = new LambdaQueryWrapper<>();
        query.eq(Artist::getArtistName, artistAddDto.getArtistName());
        if (artistMapper.selectCount(query) > 0) {
            throw new BusinessException("歌手已存在");
        }

        Artist artist = new Artist();
        BeanUtils.copyProperties(artistAddDto, artist);
        artistMapper.insert(artist);
    }

    @Override
    @Transactional
    public void updateArtist(ArtistUpdateDto artistUpdateDto) {
        Long artistId = artistUpdateDto.getArtistId();

        Artist artistByArtistName =
                artistMapper.selectOne(
                        new LambdaQueryWrapper<Artist>()
                                .eq(Artist::getArtistName, artistUpdateDto.getArtistName()));
        if (artistByArtistName != null && !artistByArtistName.getArtistId().equals(artistId)) {
            throw new BusinessException("歌手已存在");
        }

        Artist artist = new Artist();
        BeanUtils.copyProperties(artistUpdateDto, artist);
        if (artistMapper.updateById(artist) == 0) {
            throw new BusinessException("更新失败");
        }
    }

    @Override
    @Transactional
    public void updateArtistAvatar(Long artistId, String avatar) {
        Artist artist = artistMapper.selectById(artistId);
        if (artist == null) throw new BusinessException(404, "资源不存在");
        String avatarUrl = artist.getAvatar();

        artist.setAvatar(avatar);
        if (artistMapper.updateById(artist) == 0) {
            throw new BusinessException("更新失败");
        }

        UploadCleanup.afterCommit(fileStorage, avatarUrl);
    }

    @Override
    @Transactional
    public void deleteArtist(Long artistId) {

        Artist artist = artistMapper.selectById(artistId);
        if (artist == null) {
            throw new BusinessException("歌手不存在");
        }
        String avatarUrl = artist.getAvatar();

        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            UploadCleanup.afterCommit(fileStorage, avatarUrl);
        }

        if (artistMapper.deleteById(artistId) == 0) {
            throw new BusinessException("删除失败");
        }
    }

    @Override
    @Transactional
    public void deleteArtists(List<Long> artistIds) {

        List<Artist> artists = artistMapper.selectByIds(artistIds);
        List<String> avatarUrlList =
                artists.stream()
                        .map(Artist::getAvatar)
                        .filter(avatarUrl -> avatarUrl != null && !avatarUrl.isEmpty())
                        .toList();

        for (String avatarUrl : avatarUrlList) {
            UploadCleanup.afterCommit(fileStorage, avatarUrl);
        }

        if (artistMapper.deleteByIds(artistIds) == 0) {
            throw new BusinessException("删除失败");
        }
    }
}
