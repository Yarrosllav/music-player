package com.musicplayer.client.strategy;

import com.musicplayer.client.player.TrackInfo;

import java.util.*;

public class ShuffleStrategy implements PlaybackStrategy {
    private final Random random = new Random();

    @Override
    public TrackInfo getNextTrack(List<TrackInfo> queue, int currentIndex) {
        if (queue == null || queue.isEmpty()) return null;
        if (queue.size() == 1) return queue.get(0);

        int nextIndex;
        do {
            nextIndex = random.nextInt(queue.size());
        } while (nextIndex == currentIndex);

        return queue.get(nextIndex);
    }

    @Override
    public String getName() {
        return "Shuffle";
    }
}