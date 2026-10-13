package com.silver.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String username;
    private String role = "USER";
    private Integer status = 0;
    private Integer tokenVersion = 0;
    private String password;
    private String nickname;
    private String email;
    private Boolean emailVerified = false;
    private String userPic;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
