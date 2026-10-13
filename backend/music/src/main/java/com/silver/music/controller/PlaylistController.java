package com.silver.music.controller;

import com.silver.common.PageResult;
import com.silver.common.Result;
import com.silver.music.dto.PlaylistQueryDto;
import com.silver.music.service.PlaylistService;
import com.silver.music.vo.PlaylistDetailVo;
import com.silver.music.vo.PlaylistVo;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/music/public/playlist")
public class PlaylistController {

    @Autowired private PlaylistService playlistService;

    @PostMapping("/listPlaylists")
    public Result<PageResult<PlaylistVo>> listPlaylists(
            @RequestBody @Valid PlaylistQueryDto playlistDto) {
        return Result.success(playlistService.listPlaylists(playlistDto));
    }

    @GetMapping("/getRecommendedPlaylists")
    public Result<List<PlaylistVo>> getRecommendedPlaylists() {
        return Result.success(playlistService.getRecommendedPlaylists());
    }

    @GetMapping("/getPlaylistDetail/{id}")
    public Result<PlaylistDetailVo> getPlaylistDetail(@PathVariable("id") Long playlistId) {
        return Result.success(playlistService.getPlaylistDetail(playlistId));
    }
}
