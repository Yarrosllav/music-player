package com.musicplayer.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "playlist_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaylistItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "playlist_id", nullable = false)
    private Long playlistId;

    @Column(name = "track_id", nullable = false)
    private Long trackId;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "added_at")
    private LocalDateTime addedAt = LocalDateTime.now();
}
