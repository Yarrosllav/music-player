package com.musicplayer.client.player;

public class PlayingState implements PlayerState {
    @Override
    public void play(MusicPlayer player) {
    }

    @Override
    public void pause(MusicPlayer player) {
        player.setState(new PausedState());
        player.pausePlayback();
    }

    @Override
    public void stop(MusicPlayer player) {
        player.setState(new StoppedState());
        player.stopPlayback();
    }

    @Override
    public PlaybackState getState() {
        return PlaybackState.PLAYING;
    }
}
