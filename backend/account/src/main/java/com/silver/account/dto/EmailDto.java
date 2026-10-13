package com.silver.account.dto;

import lombok.Data;

@Data
public class EmailDto {
    private String email;
    private String purpose;
    private String code;
    private String newPwd;
    private String reNewPwd;
}
