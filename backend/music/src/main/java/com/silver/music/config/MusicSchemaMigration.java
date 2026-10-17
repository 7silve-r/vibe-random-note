package com.silver.music.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@Order(-50)
public class MusicSchemaMigration implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    public MusicSchemaMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer exists =
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='music_song' AND column_name='lyric_url'",
                        Integer.class);
        if (exists == null || exists == 0)
            jdbc.execute("ALTER TABLE music_song ADD COLUMN lyric_url VARCHAR(512) NULL");
        new ResourceDatabasePopulator(new ClassPathResource("db/song-artists.sql"))
                .execute(jdbc.getDataSource());
    }
}
