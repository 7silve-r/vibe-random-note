package com.silver.storage.model;

public enum UploadTarget {
    AVATAR("avatars", true, false),
    ARTICLE_COVER("covers", true, false),
    ARTIST_AVATAR("artists", false, false),
    SONG_COVER("songCovers", false, false),
    SONG_AUDIO("songs", false, true),
    SONG_LYRIC("lyrics", false, false),
    PLAYLIST_COVER("playlists", false, false),
    BANNER("banners", false, false),
    MUSIC_USER_AVATAR("users", false, false);

    private final String folder;
    private final boolean privateBucket;
    private final boolean audio;

    UploadTarget(String folder, boolean privateBucket, boolean audio) {
        this.folder = folder;
        this.privateBucket = privateBucket;
        this.audio = audio;
    }

    public String folder() {
        return folder;
    }

    public boolean privateBucket() {
        return privateBucket;
    }

    public boolean audio() {
        return audio;
    }
}
