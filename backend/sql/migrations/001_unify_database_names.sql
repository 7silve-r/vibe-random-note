CREATE DATABASE IF NOT EXISTS `vibe_random_note` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

RENAME TABLE
    `music_diary`.`user` TO `vibe_random_note`.`account_user`,
    `music_diary`.`email_code` TO `vibe_random_note`.`account_email_code`,
    `music_diary`.`category` TO `vibe_random_note`.`diary_category`,
    `music_diary`.`article` TO `vibe_random_note`.`diary_article`,
    `music_diary`.`article_like` TO `vibe_random_note`.`diary_article_like`,
    `music_diary`.`article_favorite` TO `vibe_random_note`.`diary_article_favorite`,
    `music_diary`.`article_comment` TO `vibe_random_note`.`diary_article_comment`,
    `music_diary`.`article_comment_like` TO `vibe_random_note`.`diary_article_comment_like`,
    `music_diary`.`tb_artist` TO `vibe_random_note`.`music_artist`,
    `music_diary`.`tb_banner` TO `vibe_random_note`.`music_banner`,
    `music_diary`.`tb_comment` TO `vibe_random_note`.`music_comment`,
    `music_diary`.`tb_comment_like` TO `vibe_random_note`.`music_comment_like`,
    `music_diary`.`tb_feedback` TO `vibe_random_note`.`music_feedback`,
    `music_diary`.`tb_genre` TO `vibe_random_note`.`music_genre`,
    `music_diary`.`tb_playlist` TO `vibe_random_note`.`music_playlist`,
    `music_diary`.`tb_playlist_binding` TO `vibe_random_note`.`music_playlist_binding`,
    `music_diary`.`tb_song` TO `vibe_random_note`.`music_song`,
    `music_diary`.`tb_style` TO `vibe_random_note`.`music_style`,
    `music_diary`.`tb_user_favorite` TO `vibe_random_note`.`music_user_favorite`;
