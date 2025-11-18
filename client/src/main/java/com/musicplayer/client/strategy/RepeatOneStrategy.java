package com.musicplayer.client.strategy;

import com.musicplayer.client.player.TrackInfo;

import java.util.List;

public class RepeatOneStrategy implements PlaybackStrategy {
    @Override
    public TrackInfo getNextTrack(List<TrackInfo> queue, int currentIndex) {
        if (queue == null || queue.isEmpty()) return null;
        return queue.get(currentIndex); // Repeat current track
    }

    @Override
    public String getName() {
        return "Repeat One";
    }
}
