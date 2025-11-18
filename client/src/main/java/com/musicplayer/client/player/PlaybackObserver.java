package com.musicplayer.client.player;

public interface PlaybackObserver {
    void onPlaybackStateChanged(PlaybackState state);
    void onTrackChanged(TrackInfo track);
    void onPositionChanged(double position);
    void onVolumeChanged(double volume);
}
