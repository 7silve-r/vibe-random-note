package com.silver.poetry.service;

import com.silver.common.PageResult;
import com.silver.poetry.vo.PoemVo;
import com.silver.poetry.vo.PoetryCatalogVo;

public interface PoetryService {
    PageResult<PoemVo> list(
            int pageNum,
            int pageSize,
            String keyword,
            String dynasty,
            String kind,
            String dataset,
            Integer authorId,
            boolean favorites);

    PoemVo detail(Integer id);

    PoemVo random(Integer exclude, String dataset);

    PoetryCatalogVo catalog();

    PoemVo daily();

    void favorite(Integer id, boolean cancel);
}
