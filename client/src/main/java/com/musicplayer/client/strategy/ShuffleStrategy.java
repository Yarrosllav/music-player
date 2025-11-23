package com.musicplayer.client.strategy;

import java.util.*;

public class ShuffleStrategy implements PlaybackStrategy {
    private final Random random = new Random();

    @Override
    public int getNextIndex(int currentIndex, int listSize) {
        if (listSize <= 1) return 0;
        int next;
        do {
            next = random.nextInt(listSize);
        } while (next == currentIndex);
        return next;
    }

    @Override
    public int getPreviousIndex(int currentIndex, int listSize) {
        return -1;
    }

    @Override
    public boolean isShuffle() {
        return true;
    }

    @Override
    public String getName() {
        return "Shuffle";
    }
}