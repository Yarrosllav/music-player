package com.musicplayer.client.player;

import com.musicplayer.client.strategy.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

public class MusicPlayer {
    private MediaPlayer mediaPlayer;
    private List<TrackInfo> queue = new ArrayList<>();
    private int currentTrackIndex = 0;

    private PlayerState state;
    private PlaybackStrategy playbackStrategy;
    private final List<PlaybackObserver> observers = new ArrayList<>();

    private final Stack<Integer> history = new Stack<>();
    private final double[] equalizerGains = new double[10];

    public MusicPlayer() {
        this.state = new StoppedState();
        this.playbackStrategy = new SequentialStrategy();
    }

    // state
    public void setState(PlayerState state) {
        this.state = state;
        notifyObservers(o -> o.onPlaybackStateChanged(state.getState()));
    }

    public void play() {
        state.play(this);
    }
    public void pause() {
        state.pause(this);
    }
    public void stop() {
        state.stop(this);
    }

    public void playPause() {
        if (state.getState() == PlaybackState.PLAYING) pause();
        else play();
    }

    public void startPlayback() {
        if (mediaPlayer != null) mediaPlayer.play();
    }
    public void pausePlayback() {
        if (mediaPlayer != null) mediaPlayer.pause();
    }
    public void resumePlayback() {
        if (mediaPlayer != null) mediaPlayer.play();
    }
    public void stopPlayback() {
        if (mediaPlayer != null) mediaPlayer.stop();
        notifyObservers(o -> o.onPositionChanged(0));
    }


    public void playQueue(List<TrackInfo> newQueue, int startIndex) {
        this.queue = new ArrayList<>(newQueue);
        this.currentTrackIndex = startIndex;
        this.history.clear();
        playTrackInternal(startIndex, false);
    }

    public void playTrack(TrackInfo track) {
        playQueue(Collections.singletonList(track), 0);
    }

    public void updateQueue(List<TrackInfo> newQueue) {
        if (queue.isEmpty()) {
            this.queue = new ArrayList<>(newQueue);
            return;
        }
        Long currentId = queue.get(currentTrackIndex).getId();
        this.queue = new ArrayList<>(newQueue);
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i).getId().equals(currentId)) {
                this.currentTrackIndex = i;
                break;
            }
        }
    }

    // playback
    private void playTrackInternal(int index, boolean pushToHistory) {
        if (queue.isEmpty() || index < 0 || index >= queue.size()) return;

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }

        if (pushToHistory) history.push(currentTrackIndex);
        currentTrackIndex = index;
        TrackInfo track = queue.get(index);

        try {
            Media media = createMedia(track);
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setVolume(0.5);

            mediaPlayer.setOnError(() -> {
                System.err.println("MEDIA PLAYER ERROR: " + mediaPlayer.getError());
                if (mediaPlayer.getError() != null) {
                    mediaPlayer.getError().printStackTrace();
                }
            });

            applyEqualizerSettings();

            mediaPlayer.setOnReady(() -> {
                setState(new PlayingState());
                mediaPlayer.play();
                notifyObservers(o -> o.onTrackChanged(track));
            });

            mediaPlayer.setOnEndOfMedia(this::onTrackFinished);
            mediaPlayer.currentTimeProperty().addListener((obs, oldVal, newVal) ->
                    notifyObservers(o -> o.onPositionChanged(newVal.toSeconds()))
            );

        } catch (Exception e) {
            System.err.println("Error loading media: " + e.getMessage());
            setState(new StoppedState());
        }
    }

    private Media createMedia(TrackInfo track) {
        if ("local".equals(track.getSource())) {
            return new Media(new File(track.getLocalPath()).toURI().toString());
        }
        return new Media("http://localhost:8080/api/tracks/" + track.getId() + "/stream");
    }

    private void applyEqualizerSettings() {
        if (mediaPlayer.getAudioEqualizer() == null) return;
        var bands = mediaPlayer.getAudioEqualizer().getBands();
        for (int i = 0; i < Math.min(bands.size(), equalizerGains.length); i++) {
            bands.get(i).setGain(equalizerGains[i]);
        }
    }

    private void onTrackFinished() {
        if (queue.isEmpty()) return;
        int nextIndex = playbackStrategy.getNextIndex(currentTrackIndex, queue.size());

        if (nextIndex == -1) {
            stop();
            currentTrackIndex = queue.size() - 1;
        } else {
            playTrackInternal(nextIndex, true);
        }
    }

    // navigation
    public void playNext() {
        if (queue.isEmpty()) return;

        if (playbackStrategy instanceof RepeatOneStrategy) {
            int next = (currentTrackIndex + 1) % queue.size();
            playTrackInternal(next, true);
            return;
        }

        int nextIndex = playbackStrategy.getNextIndex(currentTrackIndex, queue.size());
        if (nextIndex == -1) {
            stop();
            currentTrackIndex = queue.size() - 1;
        } else {
            playTrackInternal(nextIndex, true);
        }
    }

    public void playPrevious() {
        if (queue.isEmpty()) return;

        if (mediaPlayer != null && mediaPlayer.getCurrentTime().toSeconds() > 3) {
            mediaPlayer.seek(Duration.ZERO);
            return;
        }

        if (playbackStrategy.isShuffle() && !history.isEmpty()) {
            playTrackInternal(history.pop(), false);
            return;
        }

        if (playbackStrategy instanceof RepeatOneStrategy) {
            int prev = currentTrackIndex - 1;
            if (prev < 0) prev = queue.size() - 1;
            playTrackInternal(prev, false);
            return;
        }

        if (state.getState() == PlaybackState.STOPPED) {
            playTrackInternal(currentTrackIndex, false);
            return;
        }

        playTrackInternal(playbackStrategy.getPreviousIndex(currentTrackIndex, queue.size()), false);
    }

    public void setPlaybackStrategy(PlaybackStrategy strategy) {
        this.playbackStrategy = strategy;
        if (!strategy.isShuffle()) history.clear();
    }

    public void playLocalFile(File file) {
        playTrack(TrackInfo.builder()
                .title(file.getName())
                .artist("")
                .source("local")
                .localPath(file.getAbsolutePath())
                .build());
    }

    public void seek(double seconds) {
        if (mediaPlayer != null) mediaPlayer.seek(Duration.seconds(seconds));
    }

    public void setVolume(double volume) {
        if (mediaPlayer != null) mediaPlayer.setVolume(volume);
    }

    public double getDuration() {
        return mediaPlayer != null ? mediaPlayer.getTotalDuration().toSeconds() : 0;
    }

    public TrackInfo getCurrentTrack() {
        return (queue.isEmpty() || currentTrackIndex >= queue.size()) ? null : queue.get(currentTrackIndex);
    }

    public PlayerState getCurrentState() {
        return state;
    }

    public String getStrategyName() {
        if (playbackStrategy instanceof ShuffleStrategy) return "SHUFFLE";
        if (playbackStrategy instanceof RepeatOneStrategy) return "ONE";
        if (playbackStrategy instanceof RepeatAllStrategy) return "ALL";
        return "NONE";
    }

    // equalizer
    public void setEqualizerBand(int index, double gain) {
        if (index >= 0 && index < equalizerGains.length) {
            equalizerGains[index] = gain;
            if (mediaPlayer != null && mediaPlayer.getAudioEqualizer() != null) {
                var bands = mediaPlayer.getAudioEqualizer().getBands();
                if (index < bands.size()) bands.get(index).setGain(gain);
            }
        }
    }

    public double getEqualizerBandValue(int index) {
        return (index >= 0 && index < equalizerGains.length) ? equalizerGains[index] : 0.0;
    }

    // observer
    public void addObserver(PlaybackObserver o) {
        observers.add(o);
    }
    public void removeObserver(PlaybackObserver o) {
        observers.remove(o);
    }

    private void notifyObservers(Consumer<PlaybackObserver> action) {
        observers.forEach(action);
    }
}