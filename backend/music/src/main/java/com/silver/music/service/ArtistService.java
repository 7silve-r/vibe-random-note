package com.silver.music.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.silver.common.PageResult;
import com.silver.music.dto.ArtistAddDto;
import com.silver.music.dto.ArtistQueryDto;
import com.silver.music.dto.ArtistUpdateDto;
import com.silver.music.entity.Artist;
import com.silver.music.vo.ArtistDetailVo;
import com.silver.music.vo.ArtistNameVo;
import com.silver.music.vo.ArtistVo;
import java.util.List;

public interface ArtistService extends IService<Artist> {

    PageResult<ArtistVo> listArtists(ArtistQueryDto artistDto);

    PageResult<Artist> listAdminArtists(ArtistQueryDto artistDto);

    List<ArtistNameVo> listArtistNames();

    List<ArtistVo> getRandomArtists();

    ArtistDetailVo getArtistDetail(Long artistId);

    Long countArtists(Integer gender, String area);

    void addArtist(ArtistAddDto artistAddDto);

    void updateArtist(ArtistUpdateDto artistUpdateDto);

    void updateArtistAvatar(Long artistId, String avatar);

    void deleteArtist(Long artistId);

    void deleteArtists(List<Long> artistIds);
}
