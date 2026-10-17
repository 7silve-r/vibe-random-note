CREATE TABLE IF NOT EXISTS music_song_artist (
    song_id BIGINT NOT NULL,
    artist_id BIGINT NOT NULL,
    position INT NOT NULL,
    PRIMARY KEY (song_id, artist_id),
    INDEX song_artist_artist (artist_id),
    CONSTRAINT song_artist_song FOREIGN KEY (song_id) REFERENCES music_song(id) ON DELETE CASCADE,
    CONSTRAINT song_artist_artist_fk FOREIGN KEY (artist_id) REFERENCES music_artist(id) ON DELETE RESTRICT
);

INSERT INTO music_song_artist (song_id, artist_id, position)
SELECT s.id, s.artist_id, 0 FROM music_song s
WHERE NOT EXISTS (SELECT 1 FROM music_song_artist a WHERE a.song_id = s.id);
