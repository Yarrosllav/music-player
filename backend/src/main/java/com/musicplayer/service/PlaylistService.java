package com.musicplayer.service;

import com.musicplayer.dto.*;
import com.musicplayer.model.*;
import com.musicplayer.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlaylistService {
    private final PlaylistRepository playlistRepository;
    private final PlaylistItemRepository playlistItemRepository;
    private final TrackRepository trackRepository;

    public List<PlaylistDto> getUserPlaylists(Long userId) {
        return playlistRepository.findByUserOwnerId(userId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public PlaylistDto getPlaylistById(Long id) {
        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Playlist not found"));
        return convertToDto(playlist);
    }

    public Playlist createPlaylist(Long userId, String name) {
        Playlist playlist = Playlist.builder()
                .name(name)
                .userOwnerId(userId)
                .isPublic(false)
                .build();
        return playlistRepository.save(playlist);
    }

    @Transactional
    public void addTrackToPlaylist(Long playlistId, Long trackId) {
        List<PlaylistItem> items = playlistItemRepository
                .findByPlaylistIdOrderByPositionAsc(playlistId);

        int nextPosition = items.isEmpty() ? 0 :
                items.get(items.size() - 1).getPosition() + 1;

        PlaylistItem item = PlaylistItem.builder()
                .playlistId(playlistId)
                .trackId(trackId)
                .position(nextPosition)
                .build();

        playlistItemRepository.save(item);
    }

    @Transactional
    public void removeTrackFromPlaylist(Long playlistId, Long trackId) {
        playlistItemRepository.deleteByPlaylistIdAndTrackId(playlistId, trackId);
    }

    @Transactional
    public void deletePlaylist(Long id) {
        playlistItemRepository.deleteByPlaylistId(id);
        playlistRepository.deleteById(id);
    }

    private PlaylistDto convertToDto(Playlist playlist) {
        List<TrackDto> tracks = playlistItemRepository
                .findByPlaylistIdOrderByPositionAsc(playlist.getId()).stream()
                .map(item -> trackRepository.findById(item.getTrackId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(track -> TrackDto.builder()
                        .id(track.getId())
                        .title(track.getTitle())
                        .artist(track.getArtist())
                        .album(track.getAlbum())
                        .durationMs(track.getDurationMs())
                        .sizeBytes(track.getSizeBytes())
                        .build())
                .collect(Collectors.toList());

        return PlaylistDto.builder()
                .id(playlist.getId())
                .name(playlist.getName())
                .userOwnerId(playlist.getUserOwnerId())
                .isPublic(playlist.getIsPublic())
                .tracks(tracks)
                .build();
    }
}