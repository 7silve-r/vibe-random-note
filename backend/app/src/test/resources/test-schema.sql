CREATE TABLE IF NOT EXISTS `account_user` (
id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(30) UNIQUE, password VARCHAR(255),
nickname VARCHAR(100), email VARCHAR(254) UNIQUE, email_verified BOOLEAN DEFAULT FALSE, user_pic VARCHAR(512), role VARCHAR(10) DEFAULT 'USER',
status INT DEFAULT 0, token_version INT DEFAULT 0, create_time TIMESTAMP, update_time TIMESTAMP);

CREATE TABLE IF NOT EXISTS diary_category (id INT AUTO_INCREMENT PRIMARY KEY, cate_name VARCHAR(50), cate_alias VARCHAR(50), create_user INT, create_time TIMESTAMP, update_time TIMESTAMP);
CREATE TABLE IF NOT EXISTS diary_article (id INT AUTO_INCREMENT PRIMARY KEY, title VARCHAR(200), cate_id INT, cover_img VARCHAR(512), content CLOB, state VARCHAR(10), create_user INT, create_time TIMESTAMP, update_time TIMESTAMP);

CREATE TABLE IF NOT EXISTS diary_article_like (
    id INT PRIMARY KEY AUTO_INCREMENT,
    article_id INT NOT NULL,
    user_id INT NOT NULL,

    FOREIGN KEY (article_id) REFERENCES diary_article(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES `account_user`(id) ON DELETE CASCADE
    ,
    UNIQUE (article_id, user_id)
);

CREATE TABLE IF NOT EXISTS diary_article_favorite (
    id INT PRIMARY KEY AUTO_INCREMENT,
    article_id INT NOT NULL,
    user_id INT NOT NULL,

    FOREIGN KEY (article_id) REFERENCES diary_article(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES `account_user`(id) ON DELETE CASCADE
    ,
    UNIQUE (article_id, user_id)
);

CREATE TABLE IF NOT EXISTS diary_article_comment (
    id INT PRIMARY KEY AUTO_INCREMENT,
    article_id INT NOT NULL,
    user_id INT NOT NULL,
    content VARCHAR(255) NOT NULL, create_time TIMESTAMP NOT NULL,
    FOREIGN KEY (article_id) REFERENCES diary_article(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES `account_user`(id) ON DELETE CASCADE


);

CREATE TABLE IF NOT EXISTS diary_article_comment_like (
    id INT PRIMARY KEY AUTO_INCREMENT,
    comment_id INT NOT NULL,
    user_id INT NOT NULL,

    FOREIGN KEY (comment_id) REFERENCES diary_article_comment(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES `account_user`(id) ON DELETE CASCADE
    ,
    UNIQUE (comment_id, user_id)
);

CREATE TABLE IF NOT EXISTS account_email_code (
  id INT PRIMARY KEY AUTO_INCREMENT,
  email VARCHAR(254) NOT NULL,
  purpose VARCHAR(10) NOT NULL,
  code VARCHAR(255) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  sent_at TIMESTAMP NOT NULL,
  attempts INT NOT NULL DEFAULT 0,
  UNIQUE (email, purpose)
);

CREATE TABLE IF NOT EXISTS music_artist (
`id` bigint NOT NULL AUTO_INCREMENT,
`name` varchar(100) NOT NULL,
`gender` int NULL DEFAULT NULL,
`avatar` varchar(255) NULL DEFAULT NULL,
`birth` date NULL DEFAULT NULL,
`area` varchar(30) NULL DEFAULT NULL,
`introduction` varchar(255) NULL DEFAULT NULL,
PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS music_banner (
`id` bigint NOT NULL AUTO_INCREMENT,
`banner_url` varchar(255) NOT NULL,
`status` tinyint NOT NULL,
PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS music_comment (
`id` bigint NOT NULL AUTO_INCREMENT,
`user_id` int NOT NULL,
`song_id` bigint NULL DEFAULT NULL,
`playlist_id` bigint NULL DEFAULT NULL,
`content` varchar(255) NOT NULL,
`create_time` datetime NOT NULL,
`type` tinyint NOT NULL,
`like_count` bigint NULL DEFAULT NULL,
PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS music_feedback (
`id` bigint NOT NULL AUTO_INCREMENT,
`user_id` int NOT NULL,
`feedback` varchar(255) NOT NULL,
`create_time` datetime NOT NULL,
PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS music_genre (
`song_id` bigint NOT NULL,
`style_id` bigint NOT NULL,
PRIMARY KEY (`song_id`, `style_id`)
);

CREATE TABLE IF NOT EXISTS music_playlist (
`id` bigint NOT NULL AUTO_INCREMENT,
`title` varchar(255) NOT NULL,
`cover_url` varchar(255) NULL DEFAULT NULL,
`introduction` text NULL,
`style` varchar(255) NULL DEFAULT NULL,
PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS music_playlist_binding (
`playlist_id` bigint NOT NULL,
`song_id` bigint NOT NULL,
PRIMARY KEY (`playlist_id`, `song_id`)
);

CREATE TABLE IF NOT EXISTS music_song (
`id` bigint NOT NULL AUTO_INCREMENT,
`artist_id` bigint NOT NULL,
`name` varchar(255) NOT NULL,
`album` varchar(255) NOT NULL,
`lyric_url` varchar(512) NULL,
  `lyric` text NULL,
`duration` varchar(10) NULL DEFAULT NULL,
`style` varchar(255) NULL DEFAULT NULL,
`cover_url` varchar(255) NULL DEFAULT NULL,
`audio_url` varchar(255) NULL DEFAULT NULL,
`release_time` date NOT NULL,
PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS music_style (
`id` bigint NOT NULL AUTO_INCREMENT,
`name` varchar(50) NOT NULL,
PRIMARY KEY (`id`),
UNIQUE(`name`)
);

CREATE TABLE IF NOT EXISTS music_user_favorite (
`id` bigint NOT NULL AUTO_INCREMENT,
`user_id` int NOT NULL,
`type` tinyint NOT NULL,
`song_id` bigint NULL DEFAULT NULL,
`playlist_id` bigint NULL DEFAULT NULL,
`create_time` datetime NOT NULL,
PRIMARY KEY (`id`),
UNIQUE KEY favorite_song (user_id, song_id),
UNIQUE KEY favorite_playlist (user_id, playlist_id)
);

CREATE TABLE IF NOT EXISTS music_comment_like (
`id` bigint NOT NULL AUTO_INCREMENT,
`comment_id` bigint NOT NULL,
`user_id` int NOT NULL,
PRIMARY KEY (`id`),
UNIQUE KEY comment_user (`comment_id`, `user_id`)
);

ALTER TABLE music_comment ADD CONSTRAINT `fk_comment_playlist_id` FOREIGN KEY (`playlist_id`) REFERENCES `music_playlist` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE music_comment ADD CONSTRAINT `fk_comment_song_id` FOREIGN KEY (`song_id`) REFERENCES `music_song` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE music_comment ADD CONSTRAINT `fk_comment_user_id` FOREIGN KEY (`user_id`) REFERENCES `account_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE music_feedback ADD CONSTRAINT `fk_feedback_user_id` FOREIGN KEY (`user_id`) REFERENCES `account_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE music_genre ADD CONSTRAINT `fk_genre_song_id` FOREIGN KEY (`song_id`) REFERENCES `music_song` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT;
ALTER TABLE music_genre ADD CONSTRAINT `fk_genre_style_id` FOREIGN KEY (`style_id`) REFERENCES `music_style` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT;
ALTER TABLE music_playlist_binding ADD CONSTRAINT `fk_playlist_binding_playlist_id` FOREIGN KEY (`playlist_id`) REFERENCES `music_playlist` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT;
ALTER TABLE music_playlist_binding ADD CONSTRAINT `fk_playlist_binding_song_id` FOREIGN KEY (`song_id`) REFERENCES `music_song` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT;
ALTER TABLE music_song ADD CONSTRAINT `fk_song_artist_id` FOREIGN KEY (`artist_id`) REFERENCES `music_artist` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE music_user_favorite ADD CONSTRAINT `fk_user_favorite_playlist_id` FOREIGN KEY (`playlist_id`) REFERENCES `music_playlist` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE music_user_favorite ADD CONSTRAINT `fk_user_favorite_song_id` FOREIGN KEY (`song_id`) REFERENCES `music_song` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE music_user_favorite ADD CONSTRAINT `fk_user_favorite_user_id` FOREIGN KEY (`user_id`) REFERENCES `account_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE music_comment_like ADD CONSTRAINT comment_like_comment FOREIGN KEY (`comment_id`) REFERENCES `music_comment` (`id`) ON DELETE CASCADE;
ALTER TABLE music_comment_like ADD CONSTRAINT comment_like_user FOREIGN KEY (`user_id`) REFERENCES `account_user` (`id`) ON DELETE CASCADE;




CREATE TABLE IF NOT EXISTS poetry_author (
    id INT PRIMARY KEY AUTO_INCREMENT,
    source_key VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    original_name VARCHAR(100) NOT NULL,
    dynasty VARCHAR(10) NOT NULL,
    description MEDIUMTEXT,
    INDEX poetry_author_name (name)
);

CREATE TABLE IF NOT EXISTS poetry_poem (
    id INT PRIMARY KEY AUTO_INCREMENT,
    source_key VARCHAR(64) NOT NULL UNIQUE,
    dataset VARCHAR(20) NOT NULL,
    source_ref VARCHAR(512) NOT NULL,
    source_version VARCHAR(64) NOT NULL,
    author_id INT NOT NULL,
    author_name VARCHAR(100) NOT NULL,
    title VARCHAR(300) NOT NULL,
    original_title VARCHAR(300) NOT NULL,
    rhythmic VARCHAR(100),
    dynasty VARCHAR(10) NOT NULL,
    kind VARCHAR(10) NOT NULL,
    content MEDIUMTEXT NOT NULL,
    original_content MEDIUMTEXT NOT NULL,
    excerpt VARCHAR(1000),
    featured_rank INT UNIQUE,
    FOREIGN KEY (author_id) REFERENCES poetry_author(id),
    INDEX poetry_author_poems (author_id, id),
    INDEX poetry_filters (dynasty, kind, id),
    INDEX poetry_dataset (dataset, id)
);

CREATE TABLE IF NOT EXISTS poetry_favorite (
    user_id INT NOT NULL,
    poem_id INT NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, poem_id),
    FOREIGN KEY (user_id) REFERENCES account_user(id) ON DELETE CASCADE,
    FOREIGN KEY (poem_id) REFERENCES poetry_poem(id) ON DELETE CASCADE
);

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
