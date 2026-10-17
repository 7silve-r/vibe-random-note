package com.silver.music.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

@Data
public class SongAddDto {

    @Positive private Long artistId;

    @Size(min = 1, max = 20)
    private List<@NotNull @Positive Long> artistIds;

    @Size(max = 30)
    private List<@NotNull @Positive Long> styleIds;

    @NotBlank
    @Size(max = 100)
    private String songName;

    @NotBlank
    @Size(max = 100)
    private String album;

    private String style;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @NotNull
    private LocalDate releaseTime;
}
