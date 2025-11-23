package com.musicplayer.client.strategy;

public class RepeatOneStrategy implements PlaybackStrategy {
    @Override
    public int getNextIndex(int currentIndex, int listSize) {
        return currentIndex;
    }

    @Override
    public int getPreviousIndex(int currentIndex, int listSize) {
        return currentIndex;
    }

    @Override
    public boolean isShuffle() {
        return false;
    }

    @Override
    public String getName() {
        return "Repeat One";
    }
}
