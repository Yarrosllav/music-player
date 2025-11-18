package com.musicplayer.client.player;

import com.musicplayer.client.strategy.PlaybackStrategy;
import com.musicplayer.client.strategy.SequentialStrategy;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.AudioEqualizer;
import javafx.scene.media.EqualizerBand;
import javafx.util.Duration;
import java.io.File;
import java.util.*;

public class MusicPlayer {
    private MediaPlayer mediaPlayer;
    private PlayerState currentState;
    private List<PlaybackObserver> observers = new ArrayList<>();
    private List<TrackInfo> queue = new ArrayList<>();
    private int currentTrackIndex = -1;
    private TrackInfo currentTrack;
    private PlaybackStrategy playbackStrategy = new SequentialStrategy();
    private double volume = 0.5;
    private double[] equalizerValues = new double[8]; // 8 bands

    public MusicPlayer() {
        this.currentState = new StoppedState();
    }

    // State management
    public void setState(PlayerState state) {
        this.currentState = state;
        notifyStateChanged(state.getState());
    }

    public PlayerState getCurrentState() {
        return currentState;
    }

    // Observer pattern methods
    public void addObserver(PlaybackObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(PlaybackObserver observer) {
        observers.remove(observer);
    }

    private void notifyStateChanged(PlaybackState state) {
        for (PlaybackObserver observer : observers) {
            observer.onPlaybackStateChanged(state);
        }
    }

    private void notifyTrackChanged(TrackInfo track) {
        for (PlaybackObserver observer : observers) {
            observer.onTrackChanged(track);
        }
    }

    private void notifyPositionChanged(double position) {
        for (PlaybackObserver observer : observers) {
            observer.onPositionChanged(position);
        }
    }

    private void notifyVolumeChanged(double volume) {
        for (PlaybackObserver observer : observers) {
            observer.onVolumeChanged(volume);
        }
    }

    // Playback control (called by states)
    void startPlayback() {
        if (currentTrack == null) return;

        try {
            String mediaSource;
            if ("local".equals(currentTrack.getSource())) {
                File file = new File(currentTrack.getLocalPath());
                mediaSource = file.toURI().toString();
            } else {
                String serverUrl = com.musicplayer.client.config.AppConfig.getInstance().getServerUrl();
                mediaSource = serverUrl + "/api/tracks/" + currentTrack.getId() + "/stream";
            }

            Media media = new Media(mediaSource);
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setVolume(volume);

            // Apply equalizer settings
            applyEqualizerSettings();

            mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                notifyPositionChanged(newTime.toSeconds());
            });

            mediaPlayer.setOnEndOfMedia(() -> {
                playNext();
            });

            mediaPlayer.play();
            notifyTrackChanged(currentTrack);
        } catch (Exception e) {
            System.err.println("Error starting playback: " + e.getMessage());
            e.printStackTrace();
        }
    }

    void pausePlayback() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }

    void resumePlayback() {
        if (mediaPlayer != null) {
            mediaPlayer.play();
        }
    }

    void stopPlayback() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
        currentTrack = null;
        notifyTrackChanged(null);
    }

    // Public control methods (delegate to state)
    public void play() {
        currentState.play(this);
    }

    public void pause() {
        currentState.pause(this);
    }

    public void stop() {
        currentState.stop(this);
    }

    public void playTrack(TrackInfo track) {
        stop();
        this.currentTrack = track;
        this.currentTrackIndex = queue.indexOf(track);
        play();
    }

    public void playNext() {
        TrackInfo nextTrack = playbackStrategy.getNextTrack(queue, currentTrackIndex);
        if (nextTrack != null) {
            currentTrackIndex = queue.indexOf(nextTrack);
            playTrack(nextTrack);
        } else {
            stop();
        }
    }

    public void playPrevious() {
        if (currentTrackIndex > 0) {
            currentTrackIndex--;
            playTrack(queue.get(currentTrackIndex));
        }
    }

    public void setVolume(double volume) {
        this.volume = volume;
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volume);
        }
        notifyVolumeChanged(volume);
    }

    public void seek(double seconds) {
        if (mediaPlayer != null) {
            mediaPlayer.seek(Duration.seconds(seconds));
        }
    }

    // Equalizer methods
    public void setEqualizerBand(int bandIndex, double gain) {
        if (bandIndex >= 0 && bandIndex < equalizerValues.length) {
            equalizerValues[bandIndex] = gain;
            applyEqualizerSettings();
        }
    }

    public double getEqualizerBandValue(int bandIndex) {
        if (bandIndex >= 0 && bandIndex < equalizerValues.length) {
            return equalizerValues[bandIndex];
        }
        return 0;
    }

    private void applyEqualizerSettings() {
        if (mediaPlayer != null) {
            AudioEqualizer equalizer = mediaPlayer.getAudioEqualizer();
            if (equalizer != null) {
                equalizer.setEnabled(true);
                List<EqualizerBand> bands = equalizer.getBands();
                for (int i = 0; i < Math.min(bands.size(), equalizerValues.length); i++) {
                    bands.get(i).setGain(equalizerValues[i]);
                }
            }
        }
    }

    // Strategy pattern methods
    public void setPlaybackStrategy(PlaybackStrategy strategy) {
        this.playbackStrategy = strategy;
    }

    public PlaybackStrategy getPlaybackStrategy() {
        return playbackStrategy;
    }

    // Queue management
    public void setQueue(List<TrackInfo> queue) {
        this.queue = new ArrayList<>(queue);
        this.currentTrackIndex = -1;
    }

    public void addToQueue(TrackInfo track) {
        queue.add(track);
    }

    public List<TrackInfo> getQueue() {
        return new ArrayList<>(queue);
    }

    public TrackInfo getCurrentTrack() {
        return currentTrack;
    }

    public double getCurrentPosition() {
        if (mediaPlayer != null) {
            return mediaPlayer.getCurrentTime().toSeconds();
        }
        return 0;
    }

    public double getDuration() {
        if (mediaPlayer != null && mediaPlayer.getTotalDuration() != null) {
            return mediaPlayer.getTotalDuration().toSeconds();
        }
        return 0;
    }
}