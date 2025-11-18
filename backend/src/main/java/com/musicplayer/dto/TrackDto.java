package com.musicplayer.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackDto {
    private Long id;
    private String title;
    private String artist;
    private String album;
    private Long durationMs;
    private Long sizeBytes;
}