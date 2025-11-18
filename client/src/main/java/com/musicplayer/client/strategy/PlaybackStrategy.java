package com.musicplayer.client.strategy;

import com.musicplayer.client.player.TrackInfo;

import java.util.List;

public interface PlaybackStrategy {
    TrackInfo getNextTrack(List<TrackInfo> queue, int currentIndex);
    String getName();
}
