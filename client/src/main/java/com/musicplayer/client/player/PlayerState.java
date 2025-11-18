package com.musicplayer.client.player;

public interface PlayerState {
    void play(MusicPlayer player);
    void pause(MusicPlayer player);
    void stop(MusicPlayer player);
    PlaybackState getState();
}