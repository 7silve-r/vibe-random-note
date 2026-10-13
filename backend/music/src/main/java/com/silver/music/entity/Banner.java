package com.silver.music.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("tb_banner")
public class Banner {

    @TableId(value = "id", type = IdType.AUTO)
    private Long bannerId;

    private String bannerUrl;

    private Integer status;
}
