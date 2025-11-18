package com.musicplayer.repository;

import com.musicplayer.model.PlaylistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PlaylistItemRepository extends JpaRepository<PlaylistItem, Long> {
    List<PlaylistItem> findByPlaylistIdOrderByPositionAsc(Long playlistId);
    void deleteByPlaylistIdAndTrackId(Long playlistId, Long trackId);
    void deleteByPlaylistId(Long playlistId);
}