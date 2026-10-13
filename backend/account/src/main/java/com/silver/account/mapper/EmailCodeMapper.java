package com.silver.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.silver.account.entity.EmailCode;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface EmailCodeMapper extends BaseMapper<EmailCode> {
    @Select("SELECT * FROM email_code WHERE email = #{email} AND purpose = #{purpose} FOR UPDATE")
    EmailCode lock(@Param("email") String email, @Param("purpose") String purpose);
}
