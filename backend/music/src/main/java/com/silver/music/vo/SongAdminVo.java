package com.silver.music.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

@Data
public class SongAdminVo {

    private Long songId;

    private String artistName;

    private String songName;

    private String album;

    private String lyric;
    private String lyricUrl;
    private List<Long> artistIds;
    private List<ArtistNameVo> artists;
    private List<Long> styleIds;

    private String duration;

    private String style;

    private String coverUrl;

    private String audioUrl;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate releaseTime;
}
