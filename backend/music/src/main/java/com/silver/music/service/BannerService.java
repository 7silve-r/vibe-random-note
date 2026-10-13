package com.silver.music.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.silver.common.PageResult;
import com.silver.music.dto.BannerQueryDto;
import com.silver.music.entity.Banner;
import com.silver.music.vo.BannerVo;
import java.util.List;

public interface BannerService extends IService<Banner> {

    PageResult<BannerVo> listBanners(BannerQueryDto bannerDto);

    void addBanner(String bannerUrl);

    void updateBanner(Long bannerId, String bannerUrl);

    void updateBannerStatus(Long bannerId, boolean enabled);

    void deleteBanner(Long bannerId);

    void deleteBanners(List<Long> bannerIds);

    List<BannerVo> getBannerList();
}
