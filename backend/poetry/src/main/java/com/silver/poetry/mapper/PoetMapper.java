package com.silver.poetry.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.silver.poetry.entity.Poet;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PoetMapper extends BaseMapper<Poet> {}
