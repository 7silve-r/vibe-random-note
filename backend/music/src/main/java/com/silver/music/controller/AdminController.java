package com.silver.music.controller;

import com.silver.common.PageResult;
import com.silver.common.Result;
import com.silver.music.dto.*;
import com.silver.music.entity.Artist;
import com.silver.music.entity.Playlist;
import com.silver.music.service.*;
import com.silver.music.vo.ArtistNameVo;
import com.silver.music.vo.SongAdminVo;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/music/admin")
public class AdminController {

    @Autowired private ArtistService artistService;
    @Autowired private SongService songService;
    @Autowired private PlaylistService playlistService;

    @GetMapping("/countArtists")
    public Result<Long> countArtists(
            @RequestParam(required = false) Integer gender,
            @RequestParam(required = false) String area) {
        return Result.success(artistService.countArtists(gender, area));
    }

    @PostMapping("/listArtists")
    public Result<PageResult<Artist>> listArtists(@RequestBody @Valid ArtistQueryDto artistDto) {
        return Result.success(artistService.listAdminArtists(artistDto));
    }

    @PostMapping("/addArtist")
    public Result<Void> addArtist(@RequestBody @Valid ArtistAddDto artistAddDto) {
        artistService.addArtist(artistAddDto);
        return Result.success();
    }

    @PutMapping("/updateArtist")
    public Result<Void> updateArtist(@RequestBody @Valid ArtistUpdateDto artistUpdateDto) {
        artistService.updateArtist(artistUpdateDto);
        return Result.success();
    }

    @DeleteMapping("/deleteArtist/{id}")
    public Result<Void> deleteArtist(@PathVariable("id") Long artistId) {
        artistService.deleteArtist(artistId);
        return Result.success();
    }

    @DeleteMapping("/deleteArtists")
    public Result<Void> deleteArtists(@RequestBody List<Long> artistIds) {
        artistService.deleteArtists(artistIds);
        return Result.success();
    }

    @GetMapping("/countSongs")
    public Result<Long> countSongs(@RequestParam(required = false) String style) {
        return Result.success(songService.countSongs(style));
    }

    @GetMapping("/listArtistNames")
    public Result<List<ArtistNameVo>> listArtistNames() {
        return Result.success(artistService.listArtistNames());
    }

    @PostMapping("/listAdminSongs")
    public Result<PageResult<SongAdminVo>> listAdminSongs(
            @RequestBody @Valid AdminSongQueryDto songDto) {
        return Result.success(songService.listAdminSongs(songDto));
    }

    @PostMapping("/addSong")
    public Result<Void> addSong(@RequestBody @Valid SongAddDto songAddDto) {
        songService.addSong(songAddDto);
        return Result.success();
    }

    @PutMapping("/updateSong")
    public Result<Void> updateSong(@RequestBody @Valid SongUpdateDto songUpdateDto) {
        songService.updateSong(songUpdateDto);
        return Result.success();
    }

    @DeleteMapping("/deleteSong/{id}")
    public Result<Void> deleteSong(@PathVariable("id") Long songId) {
        songService.deleteSong(songId);
        return Result.success();
    }

    @DeleteMapping("/deleteSongs")
    public Result<Void> deleteSongs(@RequestBody List<Long> songIds) {
        songService.deleteSongs(songIds);
        return Result.success();
    }

    @GetMapping("/countPlaylists")
    public Result<Long> countPlaylists(@RequestParam(required = false) String style) {
        return Result.success(playlistService.countPlaylists(style));
    }

    @PostMapping("/listPlaylists")
    public Result<PageResult<Playlist>> listPlaylists(
            @RequestBody @Valid PlaylistQueryDto playlistDto) {
        return Result.success(playlistService.listAdminPlaylists(playlistDto));
    }

    @PostMapping("/addPlaylist")
    public Result<Void> addPlaylist(@RequestBody @Valid PlaylistAddDto playlistAddDto) {
        playlistService.addPlaylist(playlistAddDto);
        return Result.success();
    }

    @PutMapping("/updatePlaylist")
    public Result<Void> updatePlaylist(@RequestBody @Valid PlaylistUpdateDto playlistUpdateDto) {
        playlistService.updatePlaylist(playlistUpdateDto);
        return Result.success();
    }

    @DeleteMapping("/deletePlaylist/{id}")
    public Result<Void> deletePlaylist(@PathVariable("id") Long playlistId) {
        playlistService.deletePlaylist(playlistId);
        return Result.success();
    }

    @DeleteMapping("/deletePlaylists")
    public Result<Void> deletePlaylists(@RequestBody List<Long> playlistIds) {
        playlistService.deletePlaylists(playlistIds);
        return Result.success();
    }
}
