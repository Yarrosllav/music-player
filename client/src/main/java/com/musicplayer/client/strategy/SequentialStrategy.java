package com.musicplayer.client.strategy;

import com.musicplayer.client.player.TrackInfo;

import java.util.List;

public class SequentialStrategy implements PlaybackStrategy {
    @Override
    public int getNextIndex(int currentIndex, int listSize) {
        if (currentIndex + 1 >= listSize) {
            return -1; // Кінець списку (зупинка)
        }
        return currentIndex + 1;
    }

    @Override
    public int getPreviousIndex(int currentIndex, int listSize) {
        if (currentIndex - 1 < 0) {
            return 0; // Впираємось в початок
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
