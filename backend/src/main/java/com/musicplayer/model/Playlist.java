package com.musicplayer.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "playlists")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Playlist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "user_owner_id", nullable = false)
    private Long userOwnerId;

    @Column(name = "is_public")
    private Boolean isPublic = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}