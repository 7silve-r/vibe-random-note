package com.silver.poetry.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.silver.poetry.mapper")
public class PoetryConfig {}
