package com.silver.music.controller;

import com.silver.common.Result;
import com.silver.music.dto.CommentPlaylistDto;
import com.silver.music.dto.CommentSongDto;
import com.silver.music.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@PreAuthorize("hasRole('USER')")
@RestController
@RequestMapping("/music/comment")
public class CommentController {

    @Autowired private CommentService commentService;

    @PostMapping("/addSongComment")
    public Result<Void> addSongComment(@RequestBody @Valid CommentSongDto commentSongDto) {
        commentService.addSongComment(commentSongDto);
        return Result.success();
    }

    @PostMapping("/addPlaylistComment")
    public Result<Void> addPlaylistComment(
            @RequestBody @Valid CommentPlaylistDto commentPlaylistDto) {
        commentService.addPlaylistComment(commentPlaylistDto);
        return Result.success();
    }

    @PatchMapping("/likeComment/{id}")
    public Result<Void> likeComment(@PathVariable("id") Long commentId) {
        commentService.likeComment(commentId);
        return Result.success();
    }

    @PatchMapping("/cancelLikeComment/{id}")
    public Result<Void> cancelLikeComment(@PathVariable("id") Long commentId) {
        commentService.cancelLikeComment(commentId);
        return Result.success();
    }

    @DeleteMapping("/deleteComment/{id}")
    public Result<Void> deleteComment(@PathVariable("id") Long commentId) {
        commentService.deleteComment(commentId);
        return Result.success();
    }
}
