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
        // Cannot pause when stopped
    }

    @Override
    public void stop(MusicPlayer player) {
        // Already stopped
    }

    @Override
    public PlaybackState getState() {
        return PlaybackState.STOPPED;
    }
}