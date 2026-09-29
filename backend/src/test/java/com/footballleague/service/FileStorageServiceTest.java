package com.footballleague.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class FileStorageServiceTest {

    @TempDir
    private Path uploadDir;

    @Test
    void gorselLogosDizinineBenzersizIsimleKaydedilir() throws IOException {
        FileStorageService service = new FileStorageService(uploadDir.toString());
        byte[] content = {1, 2, 3};

        String storedPath = service.storeLogo(new MockMultipartFile("file", "logo.png", "image/png", content));

        assertTrue(storedPath.matches("logos/[0-9a-f-]{36}\\.png"), "Beklenmeyen yol: " + storedPath);
        assertArrayEquals(content, Files.readAllBytes(uploadDir.resolve(storedPath)));
    }

    @Test
    void gorselOlmayanDosyaReddedilir() {
        FileStorageService service = new FileStorageService(uploadDir.toString());

        assertThrows(IllegalArgumentException.class, () -> service.storeLogo(
                new MockMultipartFile("file", "notlar.txt", "text/plain", new byte[] {1})));
    }

    @Test
    void bosDosyaReddedilir() {
        FileStorageService service = new FileStorageService(uploadDir.toString());

        assertThrows(IllegalArgumentException.class, () -> service.storeLogo(
                new MockMultipartFile("file", "logo.png", "image/png", new byte[0])));
    }
}
