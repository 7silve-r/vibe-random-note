package com.silver.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.silver.account.utils.SecurityUtil;
import com.silver.common.exception.BusinessException;
import com.silver.music.dto.*;
import com.silver.music.entity.*;
import com.silver.music.mapper.*;
import com.silver.music.service.CommentService;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment>
        implements CommentService {
    @Autowired private CommentMapper commentMapper;
    @Autowired private CommentLikeMapper likeMapper;
    @Autowired private SongMapper songMapper;
    @Autowired private PlaylistMapper playlistMapper;

    private void add(Long songId, Long playlistId, String content) {
        if (content == null || content.isBlank() || content.length() > 255)
            throw new BusinessException("评论须为1到255字");
        Comment comment = new Comment();
        comment.setUserId(SecurityUtil.userId().longValue());
        comment.setSongId(songId);
        comment.setPlaylistId(playlistId);
        comment.setType(songId != null ? 0 : 1);
        comment.setContent(content);
        comment.setCreateTime(LocalDateTime.now());
        comment.setLikeCount(0L);
        commentMapper.insert(comment);
    }

    @Override
    @Transactional
    public void addSongComment(CommentSongDto dto) {
        if (dto.getSongId() == null || songMapper.lock(dto.getSongId()) == null)
            throw new BusinessException(404, "歌曲不存在");
        add(dto.getSongId(), null, dto.getContent());
    }

    @Override
    @Transactional
    public void addPlaylistComment(CommentPlaylistDto dto) {
        if (dto.getPlaylistId() == null || playlistMapper.lock(dto.getPlaylistId()) == null)
            throw new BusinessException(404, "歌单不存在");
        add(null, dto.getPlaylistId(), dto.getContent());
    }

    private Comment comment(Long id) {
        Comment comment = commentMapper.lock(id);
        if (comment == null) throw new BusinessException(404, "评论不存在");
        return comment;
    }

    private void like(Long id, boolean cancel) {
        Comment comment = comment(id);
        var query =
                new LambdaQueryWrapper<CommentLike>()
                        .eq(CommentLike::getCommentId, id)
                        .eq(CommentLike::getUserId, SecurityUtil.userId());
        if (cancel) likeMapper.delete(query);
        else if (!likeMapper.exists(query)) {
            CommentLike like = new CommentLike();
            like.setCommentId(id);
            like.setUserId(SecurityUtil.userId());
            likeMapper.insert(like);
        }
        comment.setLikeCount(
                likeMapper.selectCount(
                        new LambdaQueryWrapper<CommentLike>().eq(CommentLike::getCommentId, id)));
        commentMapper.updateById(comment);
    }

    @Override
    @Transactional
    public void likeComment(Long id) {
        like(id, false);
    }

    @Override
    @Transactional
    public void cancelLikeComment(Long id) {
        like(id, true);
    }

    @Override
    @Transactional
    public void deleteComment(Long id) {
        Comment comment = comment(id);
        if (!Objects.equals(comment.getUserId(), SecurityUtil.userId().longValue())
                && !SecurityUtil.isAdmin()) {
            throw new BusinessException(403, "只能删除自己的评论");
        }
        commentMapper.deleteById(id);
    }
}
