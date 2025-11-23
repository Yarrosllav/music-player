package com.musicplayer.client.strategy;

public interface PlaybackStrategy {
    int getNextIndex(int currentIndex, int listSize);
    String getName();
    int getPreviousIndex(int currentIndex, int listSize);
    boolean isShuffle();
}
