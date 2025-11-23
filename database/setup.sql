CREATE DATABASE IF NOT EXISTS music_player CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE music_player;

CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    role ENUM('GUEST', 'USER', 'ADMIN') DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB;

CREATE TABLE tracks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    artist VARCHAR(255),
    album VARCHAR(255),
    storage_path VARCHAR(500) NOT NULL,
    duration_ms BIGINT,
    size_bytes BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_title (title),
    INDEX idx_artist (artist),
    FULLTEXT idx_search (title, artist, album)
) ENGINE=InnoDB;

CREATE TABLE playlists (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    user_owner_id BIGINT NOT NULL,
    is_public BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_owner_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_owner (user_owner_id)
) ENGINE=InnoDB;

CREATE TABLE playlist_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    playlist_id BIGINT NOT NULL,
    track_id BIGINT NOT NULL,
    position INT NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE,
    FOREIGN KEY (track_id) REFERENCES tracks(id) ON DELETE CASCADE,
    UNIQUE KEY unique_playlist_track (playlist_id, track_id),
    INDEX idx_playlist (playlist_id),
    INDEX idx_position (playlist_id, position)
) ENGINE=InnoDB;


-- Insert default admin user (password: admin123)
INSERT INTO users (username, password_hash, email, role) VALUES 
('admin', '$2a$10$k6X7/UTpTWGwOb2/w0Ebz.oYLo67NnNIwDSafhWIPwk3HX3S1nPuy', 'admin@musicplayer.com', 'ADMIN');

INSERT INTO tracks (title, artist, album, storage_path, duration_ms, size_bytes) VALUES
('Hells Bells', 'AC/DC', 'Back in Black', '432136c8-f975-4e3d-9288-e5625863d322_AC_DC — Hells Bells.mp3', null, 4870412 ),
('Angels On My Side', 'Hippie Sabotage', 'Drifter', '8b8e0209-206a-4a8e-a98f-c9cd6d779d39_Hippie Sabotage - Angels On My Side.mp3', null, 5311443),
('Flower', 'Moby', 'Play', 'e0b616ee-f21c-4a5a-bce4-0ad8ea35706c_Moby-Flower.mp3', null, 8223435),
('Seven Nation Army', 'The White Stripes', 'Elephant', 'aa4b43f2-0bae-4da2-9f9f-e40d58418d75_The White Stripes - Seven Nation Army.mp3', null, 9302527),
('Stressed Out', 'Twenty One Pilots', 'Blurryface', 'ba5305f5-a26d-414d-9224-dc23f6370c6d_twenty-one-pilots-stressed-out.mp3', null, 8131839),
('Trip Switch', 'Nothing But Thieves', 'Nothing But Thieves', 'f5232e6c-659b-4c9f-99d0-bb57f2e6e998_Nothing But Thieves - Trip Switch.mp3', null, 7432389);