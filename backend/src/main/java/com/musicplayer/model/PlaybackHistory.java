package com.musicplayer.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "playback_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaybackHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "track_id", nullable = false)
    private Long trackId;

    @Column(name = "position_ms")
    private Long positionMs = 0L;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();
}