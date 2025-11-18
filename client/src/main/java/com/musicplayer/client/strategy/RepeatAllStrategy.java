package com.musicplayer.client.strategy;

import com.musicplayer.client.player.TrackInfo;

import java.util.List;

public class RepeatAllStrategy implements PlaybackStrategy {
    @Override
    public TrackInfo getNextTrack(List<TrackInfo> queue, int currentIndex) {
        if (queue == null || queue.isEmpty()) return null;
        int nextIndex = (currentIndex + 1) % queue.size();
        return queue.get(nextIndex);
    }

    @Override
    public String getName() {
        return "Repeat All";
    }
}
