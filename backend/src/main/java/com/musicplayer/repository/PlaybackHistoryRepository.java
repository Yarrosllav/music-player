package com.musicplayer.repository;

import com.musicplayer.model.PlaybackHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PlaybackHistoryRepository extends JpaRepository<PlaybackHistory, Long> {
    List<PlaybackHistory> findByUserIdOrderByTimestampDesc(Long userId);
}