package com.silver.music.vo;

import lombok.Data;

@Data
public class BannerVo {

    private Long bannerId;

    private String bannerUrl;

    private boolean enabled;
}
