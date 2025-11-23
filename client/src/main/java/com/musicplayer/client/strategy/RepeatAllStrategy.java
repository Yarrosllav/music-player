package com.musicplayer.client.strategy;

import com.musicplayer.client.player.TrackInfo;

import java.util.List;

public class RepeatAllStrategy implements PlaybackStrategy {
    @Override
    public int getNextIndex(int currentIndex, int listSize) {
        // Якщо кінець - йдемо на 0
        return (currentIndex + 1) % listSize;
    }

    @Override
    public int getPreviousIndex(int currentIndex, int listSize) {
        // Якщо початок - йдемо в кінець
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
