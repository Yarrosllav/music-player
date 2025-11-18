package com.musicplayer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;

@Configuration
public class StorageConfig {

    @Value("${music.storage.path}")
    private String storagePath;

    @PostConstruct
    public void init() {
        try {
            Path path = Paths.get(storagePath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                System.out.println("Created storage directory: " + storagePath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create storage directory", e);
        }
    }
}
