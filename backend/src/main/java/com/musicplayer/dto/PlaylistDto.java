package com.musicplayer.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistDto {
    private Long id;
    private String name;
    private Long userOwnerId;
    private Boolean isPublic;
    private List<TrackDto> tracks;
}