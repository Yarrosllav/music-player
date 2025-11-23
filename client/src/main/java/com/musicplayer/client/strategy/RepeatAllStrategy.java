package com.musicplayer.client.strategy;

public class RepeatAllStrategy implements PlaybackStrategy {
    @Override
    public int getNextIndex(int currentIndex, int listSize) {
        return (currentIndex + 1) % listSize;
    }

    @Override
    public int getPreviousIndex(int currentIndex, int listSize) {
        if (currentIndex - 1 < 0) return listSize - 1;
        return currentIndex - 1;
    }

    @Override
    public boolean isShuffle() {
        return false;
    }

    @Override
    public String getName() {
        return "Repeat All";
    }
}
