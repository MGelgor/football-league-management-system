package com.footballleague.service;

import java.io.IOException;
import java.util.Base64;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Logo görselini "data:image/png;base64,..." adresine çevirir; veritabanında saklanır. Yayında sunucunun diski
 * geçicidir (her deploy'da silinir), bu yüzden logolar dosya olarak tutulmaz. Eski sürümlerde dosyaya
 * kaydedilmiş logolar (Team.logoPath, /uploads) okunmaya devam eder.
 */
@Service
public class FileStorageService {

    static final long MAX_LOGO_BYTES = 300L * 1024;

    public String toDataUrl(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Logo dosyası boş olamaz");
        }
        if (file.getSize() > MAX_LOGO_BYTES) {
            throw new IllegalArgumentException("Logo dosyası en fazla 300 KB olabilir");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Sadece görsel dosyaları yüklenebilir");
        }
        try {
            return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(file.getBytes());
        } catch (IOException e) {
            throw new IllegalStateException("Logo okunamadı", e);
        }
    }
}
