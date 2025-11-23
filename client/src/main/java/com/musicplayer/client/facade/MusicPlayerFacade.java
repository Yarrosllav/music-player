package com.musicplayer.client.facade;

import com.musicplayer.client.config.AppConfig;
import com.musicplayer.client.player.*;
import com.musicplayer.client.service.*;
import com.musicplayer.client.strategy.*;

import java.io.File;
import java.util.List;

public class MusicPlayerFacade {
    private static MusicPlayerFacade instance;

    private final MusicPlayer player;
    private final ApiService apiService;

    private MusicPlayerFacade() {
        this.player = new MusicPlayer();
        this.apiService = new ApiService();
    }

    public static MusicPlayerFacade getInstance() {
        if (instance == null) instance = new MusicPlayerFacade();
        return instance;
    }

    // Controls
    public void playTrack(TrackInfo track) {
        player.playTrack(track);
    }
    public void playPause() {
        player.playPause();
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
    public void setVolume(double v) {
        player.setVolume(v);
    }
    public void seek(double p) {
        player.seek(p);
    }


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

    public String getRepeatMode() {
        return player.getStrategyName();
    }

    public void playQueue(List<TrackInfo> tracks, int start) {
        player.playQueue(tracks, start);
    }
    public void updateQueue(List<TrackInfo> tracks) {
        player.updateQueue(tracks);
    }
    public void playLocalFile(File file) {
        player.playLocalFile(file);
    }

    public List<TrackInfo> searchTracks(String q) {
        return apiService.searchTracks(q);
    }
    public List<TrackInfo> getAllTracks() {
        return apiService.getAllTracks();
    }

    public void addPlaybackObserver(PlaybackObserver o) {
        player.addObserver(o);
    }
    public void removePlaybackObserver(PlaybackObserver o) {
        player.removeObserver(o);
    }

    public TrackInfo getCurrentTrack() {
        return player.getCurrentTrack();
    }
    public double getDuration() {
        return player.getDuration();
    }

    public void setEqualizerBand(int i, double g) {
        player.setEqualizerBand(i, g);
    }
    public double getEqualizerBandValue(int i) {
        return player.getEqualizerBandValue(i);
    }
}