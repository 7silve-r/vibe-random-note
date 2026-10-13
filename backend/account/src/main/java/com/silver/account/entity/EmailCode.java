package com.silver.account.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("email_code")
public class EmailCode {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String email;
    private String purpose;
    private String code;
    private LocalDateTime expiresAt;
    private LocalDateTime sentAt;
    private Integer attempts;
}
