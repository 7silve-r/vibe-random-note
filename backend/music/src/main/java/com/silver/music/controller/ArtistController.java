package com.silver.music.controller;

import com.silver.common.PageResult;
import com.silver.common.Result;
import com.silver.music.dto.ArtistQueryDto;
import com.silver.music.service.ArtistService;
import com.silver.music.vo.ArtistDetailVo;
import com.silver.music.vo.ArtistVo;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/music/public/artist")
public class ArtistController {

    @Autowired private ArtistService artistService;

    @PostMapping("/listArtists")
    public Result<PageResult<ArtistVo>> listArtists(@RequestBody @Valid ArtistQueryDto artistDto) {
        return Result.success(artistService.listArtists(artistDto));
    }

    @GetMapping("/getRandomArtists")
    public Result<List<ArtistVo>> getRandomArtists() {
        return Result.success(artistService.getRandomArtists());
    }

    @GetMapping("/getArtistDetail/{id}")
    public Result<ArtistDetailVo> getArtistDetail(@PathVariable("id") Long artistId) {
        return Result.success(artistService.getArtistDetail(artistId));
    }
}
