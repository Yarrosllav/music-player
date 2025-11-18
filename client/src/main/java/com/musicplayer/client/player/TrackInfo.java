package com.musicplayer.client.player;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackInfo {
    private Long id;
    private String title;
    private String artist;
    private String album;
    private Long durationMs;
    private String source; // "server" or "local"
    private String localPath; // for local files
}