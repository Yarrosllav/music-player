package com.musicplayer.client.strategy;

public class SequentialStrategy implements PlaybackStrategy {
    @Override
    public int getNextIndex(int currentIndex, int listSize) {
        if (currentIndex + 1 >= listSize) {
            return -1;
        }
        return currentIndex + 1;
    }

    @Override
    public int getPreviousIndex(int currentIndex, int listSize) {
        if (currentIndex - 1 < 0) {
            return 0;
        }
        return currentIndex - 1;
    }

    @Override
    public boolean isShuffle() {
        return false;
    }

    @Override
    public String getName() {
        return "Sequential";
    }
}
