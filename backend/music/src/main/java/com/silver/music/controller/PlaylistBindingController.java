package com.silver.music.controller;

import com.silver.common.Result;
import com.silver.music.service.PlaylistBindingService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class PlaylistBindingController {
    @Autowired private PlaylistBindingService playlistBindingService;

    @PutMapping("/music/admin/playlists/{id}/songs")
    public Result<Void> update(@PathVariable Long id, @RequestBody List<Long> songIds) {
        playlistBindingService.update(id, songIds);
        return Result.success();
    }
}
