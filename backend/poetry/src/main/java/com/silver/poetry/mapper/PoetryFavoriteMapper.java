package com.silver.poetry.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface PoetryFavoriteMapper {
    @Select(
            "SELECT COUNT(*) FROM poetry_favorite WHERE user_id = #{userId} AND poem_id = #{poemId}")
    int count(@Param("userId") Long userId, @Param("poemId") Integer poemId);

    @Insert("INSERT INTO poetry_favorite (user_id, poem_id) VALUES (#{userId}, #{poemId})")
    void insert(@Param("userId") Integer userId, @Param("poemId") Integer poemId);

    @Delete("DELETE FROM poetry_favorite WHERE user_id = #{userId} AND poem_id = #{poemId}")
    void delete(@Param("userId") Integer userId, @Param("poemId") Integer poemId);
}
