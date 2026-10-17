package com.silver.music.controller;

import com.silver.common.Result;
import com.silver.common.exception.BusinessException;
import com.silver.music.service.*;
import com.silver.storage.model.UploadResult;
import com.silver.storage.model.UploadTarget;
import com.silver.storage.service.FileUploadService;
import com.silver.storage.validation.LyricFile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class MediaUploadController {
    private final FileUploadService uploads;
    private final ArtistService artists;
    private final SongService songs;
    private final PlaylistService playlists;
    private final BannerService banners;

    public MediaUploadController(
            FileUploadService uploads,
            ArtistService artists,
            SongService songs,
            PlaylistService playlists,
            BannerService banners) {
        this.uploads = uploads;
        this.artists = artists;
        this.songs = songs;
        this.playlists = playlists;
        this.banners = banners;
    }

    @PutMapping(value = "/music/admin/artists/{id}/avatar", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UploadResult> artistAvatar(
            @PathVariable("id") Long id, @RequestParam("file") MultipartFile file) {
        return Result.success(
                uploads.upload(
                        file,
                        UploadTarget.ARTIST_AVATAR,
                        url -> artists.updateArtistAvatar(id, url)));
    }

    @PutMapping(value = "/music/admin/songs/{id}/cover", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UploadResult> songCover(
            @PathVariable("id") Long id, @RequestParam("file") MultipartFile file) {
        return Result.success(
                uploads.upload(
                        file, UploadTarget.SONG_COVER, url -> songs.updateSongCover(id, url)));
    }

    @PutMapping(value = "/music/admin/songs/{id}/lyric", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UploadResult> songLyric(
            @PathVariable("id") Long id, @RequestParam("file") MultipartFile file) {
        String lyric = LyricFile.read(file);
        return Result.success(
                uploads.upload(
                        file,
                        UploadTarget.SONG_LYRIC,
                        url -> songs.updateSongLyric(id, url, lyric)));
    }

    @DeleteMapping("/music/admin/songs/{id}/lyric")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteLyric(@PathVariable("id") Long id) {
        songs.updateSongLyric(id, null, null);
        return Result.success();
    }

    @PutMapping(value = "/music/admin/songs/{id}/audio", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UploadResult> songAudio(
            @PathVariable("id") Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("duration") String duration) {
        try {
            double seconds = Double.parseDouble(duration);
            if (!Double.isFinite(seconds) || seconds <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            throw new BusinessException("音频时长必须为正数秒数");
        }
        return Result.success(
                uploads.upload(
                        file,
                        UploadTarget.SONG_AUDIO,
                        url -> songs.updateSongAudio(id, url, duration)));
    }

    @PutMapping(value = "/music/admin/playlists/{id}/cover", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UploadResult> playlistCover(
            @PathVariable("id") Long id, @RequestParam("file") MultipartFile file) {
        return Result.success(
                uploads.upload(
                        file,
                        UploadTarget.PLAYLIST_COVER,
                        url -> playlists.updatePlaylistCover(id, url)));
    }

    @PostMapping(value = "/music/admin/banners", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UploadResult> addBanner(@RequestParam("file") MultipartFile file) {
        return Result.success(
                uploads.upload(file, UploadTarget.BANNER, url -> banners.addBanner(url)));
    }

    @PutMapping(value = "/music/admin/banners/{id}/image", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UploadResult> bannerImage(
            @PathVariable("id") Long id, @RequestParam("file") MultipartFile file) {
        return Result.success(
                uploads.upload(file, UploadTarget.BANNER, url -> banners.updateBanner(id, url)));
    }
}
