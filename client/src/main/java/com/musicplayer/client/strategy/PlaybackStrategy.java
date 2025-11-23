package com.musicplayer.client.strategy;

import com.musicplayer.client.player.TrackInfo;

import java.util.List;

public interface PlaybackStrategy {
    int getNextIndex(int currentIndex, int listSize);
    String getName();
    int getPreviousIndex(int currentIndex, int listSize);

    // Маркер, чи є стратегія випадковою (щоб плеєр знав, чи використовувати історію)
    boolean isShuffle();
}
