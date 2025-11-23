package com.musicplayer.client.player;

public class PausedState implements PlayerState {
    @Override
    public void play(MusicPlayer player) {
        player.setState(new PlayingState());
        player.resumePlayback();
    }

    @Override
    public void pause(MusicPlayer player) {
    }

    @Override
    public void stop(MusicPlayer player) {
        player.setState(new StoppedState());
        player.stopPlayback();
    }

    @Override
    public PlaybackState getState() {
        return PlaybackState.PAUSED;
    }
}
