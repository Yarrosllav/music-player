package com.musicplayer.controller;

import com.musicplayer.dto.TrackDto;
import com.musicplayer.model.Track;
import com.musicplayer.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tracks")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TrackController {
    private final TrackService trackService;
    private final StorageService storageService;

    @GetMapping
    public ResponseEntity<List<TrackDto>> getAllTracks() {
        return ResponseEntity.ok(trackService.getAllTracks());
    }

    @GetMapping("/search")
    public ResponseEntity<List<TrackDto>> searchTracks(@RequestParam String query) {
        return ResponseEntity.ok(trackService.searchTracks(query));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Track> getTrack(@PathVariable Long id) {
        return ResponseEntity.ok(trackService.getTrackById(id));
    }

    @GetMapping("/{id}/stream")
    public ResponseEntity<Resource> streamTrack(@PathVariable Long id) {
        try {
            Track track = trackService.getTrackById(id);
            Path file = storageService.load(track.getStoragePath());
            Resource resource = new UrlResource(file.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("audio/mpeg"))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + track.getTitle() + ".mp3\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createTrack(
            @RequestParam String title,
            @RequestParam String artist,
            @RequestParam String album,
            @RequestParam MultipartFile file) {
        try {
            Track track = trackService.createTrack(title, artist, album, file);
            return ResponseEntity.ok(track);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Track> updateTrack(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam String artist,
            @RequestParam String album) {
        return ResponseEntity.ok(trackService.updateTrack(id, title, artist, album));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTrack(@PathVariable Long id) {
        trackService.deleteTrack(id);
        return ResponseEntity.ok(Map.of("message", "Track deleted successfully"));
    }
}