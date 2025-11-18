package com.musicplayer.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tracks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Track {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String artist;
    private String album;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
