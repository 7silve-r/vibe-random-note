package com.silver.diary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.silver.diary.entity.Article;
import org.apache.ibatis.annotations.Select;

public interface ArticleMapper extends BaseMapper<Article> {
    @Select("SELECT * FROM diary_article WHERE id = #{id} FOR UPDATE")
    Article lock(Integer id);
}
