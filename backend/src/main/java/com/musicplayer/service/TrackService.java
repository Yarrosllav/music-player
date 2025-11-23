package com.musicplayer.service;

import com.musicplayer.dto.TrackDto;
import com.musicplayer.model.Track;
import com.musicplayer.repository.PlaylistItemRepository;
import com.musicplayer.repository.TrackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrackService {
    private final TrackRepository trackRepository;
    private final StorageService storageService;
    private final PlaylistItemRepository playlistItemRepository;

    public List<TrackDto> getAllTracks() {
        return trackRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<TrackDto> searchTracks(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllTracks();
        }
        return trackRepository.searchTracks(query).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Track getTrackById(Long id) {
        return trackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Track not found"));
    }

    public Track createTrack(String title, String artist, String album,
                             MultipartFile file) throws IOException {
        String storagePath = storageService.store(file);

        Track track = Track.builder()
                .title(title)
                .artist(artist)
                .album(album)
                .storagePath(storagePath)
                .sizeBytes(file.getSize())
                .build();

        return trackRepository.save(track);
    }

    public Track updateTrack(Long id, String title, String artist, String album) {
        Track track = getTrackById(id);

        if (title != null && !title.isEmpty()) track.setTitle(title);
        if (artist != null) track.setArtist(artist);
        if (album != null) track.setAlbum(album);

        return trackRepository.save(track);
    }

    @Transactional
    public void deleteTrack(Long id) {
        Track track = getTrackById(id);
        playlistItemRepository.deleteByTrackId(id);
        if (track.getStoragePath() != null) {
            storageService.delete(track.getStoragePath());
        }
        trackRepository.delete(track);
    }

    private TrackDto convertToDto(Track track) {
        return TrackDto.builder()
                .id(track.getId())
                .title(track.getTitle())
                .artist(track.getArtist())
                .album(track.getAlbum())
                .durationMs(track.getDurationMs())
                .sizeBytes(track.getSizeBytes())
                .build();
    }
}