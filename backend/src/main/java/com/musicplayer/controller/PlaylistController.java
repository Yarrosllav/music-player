package com.musicplayer.controller;

import com.musicplayer.dto.PlaylistDto;
import com.musicplayer.model.Playlist;
import com.musicplayer.service.PlaylistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/playlists")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PlaylistController {
    private final PlaylistService playlistService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PlaylistDto>> getUserPlaylists(@PathVariable Long userId) {
        return ResponseEntity.ok(playlistService.getUserPlaylists(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlaylistDto> getPlaylist(@PathVariable Long id) {
        return ResponseEntity.ok(playlistService.getPlaylistById(id));
    }

    @PostMapping
    public ResponseEntity<Playlist> createPlaylist(
            @RequestParam Long userId,
            @RequestParam String name) {
        return ResponseEntity.ok(playlistService.createPlaylist(userId, name));
    }

    @PostMapping("/{playlistId}/tracks/{trackId}")
    public ResponseEntity<?> addTrack(
            @PathVariable Long playlistId,
            @PathVariable Long trackId) {
        playlistService.addTrackToPlaylist(playlistId, trackId);
        return ResponseEntity.ok(Map.of("message", "Track added to playlist"));
    }

    @DeleteMapping("/{playlistId}/tracks/{trackId}")
    public ResponseEntity<?> removeTrack(
            @PathVariable Long playlistId,
            @PathVariable Long trackId) {
        playlistService.removeTrackFromPlaylist(playlistId, trackId);
        return ResponseEntity.ok(Map.of("message", "Track removed from playlist"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePlaylist(@PathVariable Long id) {
        playlistService.deletePlaylist(id);
        return ResponseEntity.ok(Map.of("message", "Playlist deleted successfully"));
    }

    @PutMapping("/{id}/rename")
    public ResponseEntity<Playlist> renamePlaylist(
            @PathVariable Long id,
            @RequestParam String name) {
        Playlist playlist = playlistService.renamePlaylist(id, name);
        return ResponseEntity.ok(playlist);
    }

    @PutMapping("/{id}/tracks/order")
    public ResponseEntity<?> reorderTracks(
            @PathVariable Long id,
            @RequestBody List<Long> trackIds) {

        playlistService.updateTrackOrder(id, trackIds);
        return ResponseEntity.ok(Map.of("message", "Playlist order updated"));
    }
}