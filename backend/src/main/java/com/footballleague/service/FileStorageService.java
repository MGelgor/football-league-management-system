package com.footballleague.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private final Path logoDir;

    public FileStorageService(@Value("${app.upload-dir}") String uploadDir) {
        this.logoDir = Path.of(uploadDir, "logos");
    }

    public String storeLogo(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Logo dosyası boş olamaz");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Sadece görsel dosyaları yüklenebilir");
        }

        try {
            Files.createDirectories(logoDir);
            String extension = extractExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + extension;
            file.transferTo(logoDir.resolve(filename));
            return "logos/" + filename;
        } catch (IOException e) {
            throw new IllegalStateException("Logo kaydedilemedi", e);
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dotIndex = originalFilename.lastIndexOf('.');
        return dotIndex == -1 ? "" : originalFilename.substring(dotIndex);
    }
}
