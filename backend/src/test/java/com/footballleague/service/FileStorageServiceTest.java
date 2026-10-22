package com.footballleague.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class FileStorageServiceTest {

    private final FileStorageService service = new FileStorageService();

    @Test
    void gorselDataAdresineCevrilir() {
        String dataUrl = service.toDataUrl(new MockMultipartFile("file", "logo.png", "image/png", new byte[] {1, 2, 3}));

        assertEquals("data:image/png;base64,AQID", dataUrl);
    }

    @Test
    void gorselOlmayanDosyaReddedilir() {
        assertThrows(IllegalArgumentException.class, () -> service.toDataUrl(
                new MockMultipartFile("file", "notlar.txt", "text/plain", new byte[] {1})));
    }

    @Test
    void bosVeBuyukDosyaReddedilir() {
        assertThrows(IllegalArgumentException.class, () -> service.toDataUrl(
                new MockMultipartFile("file", "logo.png", "image/png", new byte[0])));
        assertThrows(IllegalArgumentException.class, () -> service.toDataUrl(new MockMultipartFile("file",
                "logo.png", "image/png", new byte[(int) FileStorageService.MAX_LOGO_BYTES + 1])));
    }
}
