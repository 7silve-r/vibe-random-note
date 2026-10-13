package com.silver.music.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import lombok.Data;

@Data
public class SongVo {

    private Long songId;

    private String songName;

    private String artistName;

    private String album;

    private String duration;

    private String coverUrl;

    private String audioUrl;

    private boolean collected;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate releaseTime;
}
