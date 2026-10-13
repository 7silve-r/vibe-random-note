package com.silver.music.controller;

import com.silver.common.PageResult;
import com.silver.common.Result;
import com.silver.music.dto.SongQueryDto;
import com.silver.music.service.SongService;
import com.silver.music.vo.SongDetailVo;
import com.silver.music.vo.SongVo;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/music/public/song")
public class SongController {

    @Autowired private SongService songService;

    @PostMapping("/listSongs")
    public Result<PageResult<SongVo>> listSongs(@RequestBody @Valid SongQueryDto songDto) {
        return Result.success(songService.listSongs(songDto));
    }

    @GetMapping("/getRecommendedSongs")
    public Result<List<SongVo>> getRecommendedSongs() {
        return Result.success(songService.getRecommendedSongs());
    }

    @GetMapping("/getSongDetail/{id}")
    public Result<SongDetailVo> getSongDetail(@PathVariable("id") Long songId) {
        return Result.success(songService.getSongDetail(songId));
    }
}
