package com.footballleague.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.footballleague.dto.LeagueSnapshot;
import com.footballleague.dto.SaveSlotResponse;
import com.footballleague.exception.SaveSlotNotFoundException;

import tools.jackson.databind.ObjectMapper;

/**
 * Adlandırılmış kayıt noktaları: her biri save-dir klasöründe bir JSON yedek (ad.json) ve listede gösterilen
 * kısa bilgi (ad.meta.json). Dosyada tutulduğu için bellek içi H2'de de uygulama yeniden başlayınca kaybolmaz.
 */
@Service
public class SaveSlotService {

    private static final String DATA_SUFFIX = ".json";
    private static final String META_SUFFIX = ".meta.json";

    private final BackupService backupService;
    private final ObjectMapper objectMapper;
    private final Path saveDir;

    public SaveSlotService(BackupService backupService, ObjectMapper objectMapper,
            @Value("${app.save-dir}") String saveDir) {
        this.backupService = backupService;
        this.objectMapper = objectMapper;
        this.saveDir = Path.of(saveDir);
    }

    /** En yeni kayıt başta. */
    public List<SaveSlotResponse> list() {
        if (!Files.isDirectory(saveDir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(saveDir)) {
            return files.filter(path -> path.getFileName().toString().endsWith(META_SUFFIX))
                    .map(path -> objectMapper.readValue(path.toFile(), SaveSlotResponse.class))
                    .sorted(Comparator.comparing(SaveSlotResponse::savedAt).reversed())
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Kayıtlar okunamadı", e);
        }
    }

    /** Aynı adlı kayıt varsa üzerine yazılır. */
    public SaveSlotResponse save(String name) {
        String cleanName = name.trim();
        LeagueSnapshot snapshot = backupService.export();
        try {
            Files.createDirectories(saveDir);
            Path data = dataFile(cleanName);
            try (OutputStream out = Files.newOutputStream(data)) {
                objectMapper.writeValue(out, snapshot);
            }
            SaveSlotResponse slot = new SaveSlotResponse(cleanName, snapshot.exportedAt(), Files.size(data),
                    snapshot.summary());
            objectMapper.writeValue(metaFile(cleanName).toFile(), slot);
            return slot;
        } catch (IOException e) {
            throw new IllegalStateException("Kayıt yazılamadı", e);
        }
    }

    public void load(String name) {
        Path data = existingDataFile(name);
        try (InputStream in = Files.newInputStream(data)) {
            backupService.importSnapshot(objectMapper.readValue(in, LeagueSnapshot.class));
        } catch (IOException e) {
            throw new IllegalStateException("Kayıt okunamadı", e);
        }
    }

    public void delete(String name) {
        Path data = existingDataFile(name);
        try {
            Files.delete(data);
            Files.deleteIfExists(metaFile(name));
        } catch (IOException e) {
            throw new IllegalStateException("Kayıt silinemedi", e);
        }
    }

    private Path existingDataFile(String name) {
        Path data = dataFile(name);
        if (!Files.isRegularFile(data)) {
            throw new SaveSlotNotFoundException(name);
        }
        return data;
    }

    private Path dataFile(String name) {
        return resolve(name + DATA_SUFFIX);
    }

    private Path metaFile(String name) {
        return resolve(name + META_SUFFIX);
    }

    /** Ad yol ayırıcısı içeremez (SaveSlotRequest doğrular); yine de klasör dışına çıkılmasına izin verilmez. */
    private Path resolve(String fileName) {
        Path file = saveDir.resolve(fileName).normalize();
        if (!saveDir.normalize().equals(file.getParent() == null ? Path.of("") : file.getParent())) {
            throw new IllegalArgumentException("Geçersiz kayıt adı");
        }
        return file;
    }
}
