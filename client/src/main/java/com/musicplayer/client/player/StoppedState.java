package com.musicplayer.client.player;

public class StoppedState implements PlayerState {
    @Override
    public void play(MusicPlayer player) {
        if (player.getCurrentTrack() != null) {
            player.setState(new PlayingState());
            player.startPlayback();
        }
    }

    @Override
    public void pause(MusicPlayer player) {
    }

    @Override
    public void stop(MusicPlayer player) {
    }

    @Override
    public PlaybackState getState() {
        return PlaybackState.STOPPED;
    }
}