package com.musicplayer.client.strategy;

import com.musicplayer.client.player.TrackInfo;

import java.util.List;

public class SequentialStrategy implements PlaybackStrategy {
    @Override
    public TrackInfo getNextTrack(List<TrackInfo> queue, int currentIndex) {
        if (queue == null || queue.isEmpty()) return null;
        int nextIndex = currentIndex + 1;
        if (nextIndex < queue.size()) {
            return queue.get(nextIndex);
        }
        return null; // End of queue
    }

    @Override
    public String getName() {
        return "Sequential";
    }
}
