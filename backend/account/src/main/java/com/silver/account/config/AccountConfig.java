package com.silver.account.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.silver.account.mapper")
public class AccountConfig {}
