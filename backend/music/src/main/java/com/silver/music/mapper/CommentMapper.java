package com.silver.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.silver.music.entity.Comment;
import com.silver.music.vo.CommentVo;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CommentMapper extends BaseMapper<Comment> {
    @Select("SELECT *, id AS comment_id FROM music_comment WHERE id = #{id} FOR UPDATE")
    Comment lock(Long id);

    @Select(
            """
            SELECT c.id AS commentId, u.username, u.user_pic AS userAvatar,
                   c.content, c.create_time AS createTime, c.like_count AS likeCount
            FROM music_comment c LEFT JOIN `account_user` u ON c.user_id = u.id
            WHERE c.type = #{type}
              AND ((#{type} = 0 AND c.song_id = #{id}) OR (#{type} = 1 AND c.playlist_id = #{id}))
            ORDER BY c.create_time, c.id
            """)
    List<CommentVo> listByTarget(@Param("id") Long id, @Param("type") int type);
}
