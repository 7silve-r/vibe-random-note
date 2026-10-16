package com.silver.poetry.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.silver.poetry.entity.Poem;
import com.silver.poetry.vo.PoetryGroup;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface PoemMapper extends BaseMapper<Poem> {
    @Select(
            "SELECT dataset, dynasty, kind, COUNT(*) AS total FROM poetry_poem GROUP BY dataset, dynasty, kind ORDER BY dataset, dynasty, kind")
    List<PoetryGroup> groups();

    @Select("SELECT * FROM poetry_poem WHERE id = #{id} FOR UPDATE")
    Poem lock(Integer id);
}
