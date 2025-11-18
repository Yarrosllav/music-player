package com.musicplayer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.*;
import java.util.UUID;

@Service
public class StorageService {
    @Value("${music.storage.path}")
    private String storagePath;

    public String store(MultipartFile file) throws IOException {
        Path storageDir = Paths.get(storagePath);
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
        }

        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path destination = storageDir.resolve(filename);
        Files.copy(file.getInputStream(), destination,
                StandardCopyOption.REPLACE_EXISTING);

        return filename;
    }

    public Path load(String filename) {
        return Paths.get(storagePath).resolve(filename);
    }

    public void delete(String filename) {
        try {
            Path file = load(filename);
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file", e);
        }
    }
}