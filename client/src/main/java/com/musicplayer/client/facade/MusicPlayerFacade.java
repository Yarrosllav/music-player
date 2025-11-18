package com.musicplayer.client.facade;

import com.musicplayer.client.config.AppConfig;
import com.musicplayer.client.player.*;
import com.musicplayer.client.service.*;
import com.musicplayer.client.strategy.RepeatAllStrategy;
import com.musicplayer.client.strategy.RepeatOneStrategy;
import com.musicplayer.client.strategy.SequentialStrategy;
import com.musicplayer.client.strategy.ShuffleStrategy;

import java.io.File;
import java.util.List;

/**
 * Facade pattern: Provides a simplified interface to the complex music player subsystem
 */
public class MusicPlayerFacade {
    private static MusicPlayerFacade instance;

    private final MusicPlayer player;
    private final ApiService apiService;
    private final AppConfig config;

    private MusicPlayerFacade() {
        this.player = new MusicPlayer();
        this.apiService = new ApiService();
        this.config = AppConfig.getInstance();
    }

    public static MusicPlayerFacade getInstance() {
        if (instance == null) {
            instance = new MusicPlayerFacade();
        }
        return instance;
    }

    // Simplified playback controls
    public void playTrack(TrackInfo track) {
        player.playTrack(track);
    }

    public void playPause() {
        if (player.getCurrentState().getState() == PlaybackState.PLAYING) {
            player.pause();
        } else {
            player.play();
        }
    }

    public void stop() {
        player.stop();
    }

    public void next() {
        player.playNext();
    }

    public void previous() {
        player.playPrevious();
    }

    public void setVolume(double volume) {
        player.setVolume(volume);
    }

    public void seek(double position) {
        player.seek(position);
    }

    // Playback modes
    public void setShuffleMode(boolean enabled) {
        if (enabled) {
            player.setPlaybackStrategy(new ShuffleStrategy());
        } else {
            player.setPlaybackStrategy(new SequentialStrategy());
        }
    }

    public void setRepeatMode(String mode) {
        switch (mode) {
            case "ONE":
                player.setPlaybackStrategy(new RepeatOneStrategy());
                break;
            case "ALL":
                player.setPlaybackStrategy(new RepeatAllStrategy());
                break;
            default:
                player.setPlaybackStrategy(new SequentialStrategy());
        }
    }

    // Queue management
    public void playQueue(List<TrackInfo> tracks, int startIndex) {
        player.setQueue(tracks);
        if (startIndex >= 0 && startIndex < tracks.size()) {
            player.playTrack(tracks.get(startIndex));
        }
    }

    public void addToQueue(TrackInfo track) {
        player.addToQueue(track);
    }

    // Local file playback
    public void playLocalFile(File file) {
        TrackInfo track = TrackInfo.builder()
                .title(file.getName())
                .source("local")
                .localPath(file.getAbsolutePath())
                .build();
        playTrack(track);
    }

    // Server interaction (simplified)
    public List<TrackInfo> searchTracks(String query) {
        return apiService.searchTracks(query);
    }

    public List<TrackInfo> getAllTracks() {
        return apiService.getAllTracks();
    }

    // Observer management
    public void addPlaybackObserver(PlaybackObserver observer) {
        player.addObserver(observer);
    }

    public void removePlaybackObserver(PlaybackObserver observer) {
        player.removeObserver(observer);
    }

    // State queries
    public PlaybackState getPlaybackState() {
        return player.getCurrentState().getState();
    }

    public TrackInfo getCurrentTrack() {
        return player.getCurrentTrack();
    }

    public double getCurrentPosition() {
        return player.getCurrentPosition();
    }

    public double getDuration() {
        return player.getDuration();
    }

    // Equalizer control
    public void setEqualizerBand(int bandIndex, double gain) {
        player.setEqualizerBand(bandIndex, gain);
    }

    public double getEqualizerBandValue(int bandIndex) {
        return player.getEqualizerBandValue(bandIndex);
    }
}