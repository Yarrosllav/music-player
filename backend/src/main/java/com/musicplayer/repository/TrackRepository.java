package com.musicplayer.repository;

import com.musicplayer.model.Track;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TrackRepository extends JpaRepository<Track, Long> {
    @Query("SELECT t FROM Track t WHERE " +
            "LOWER(t.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(t.artist) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(t.album) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Track> searchTracks(@Param("query") String query);

    List<Track> findByArtistContainingIgnoreCase(String artist);
    List<Track> findByAlbumContainingIgnoreCase(String album);
}