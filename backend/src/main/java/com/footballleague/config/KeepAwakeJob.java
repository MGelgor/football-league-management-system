package com.footballleague.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Render'ın ücretsiz sunucusu 15 dakika dışarıdan istek gelmezse uyur; GitHub'ın zamanlanmış işleri saatlerce
 * gecikebildiği için sunucu kendi genel adresine 10 dakikada bir istek atar (Render'ın vekil sunucusundan
 * geçtiği için dışarıdan gelmiş sayılır). /health veritabanına gitmez: Neon yine uyuyabilir.
 * Adres Render'ın verdiği RENDER_EXTERNAL_URL'dir; boşsa (yerelde, testlerde) iş kurulmaz.
 */
@Configuration
@EnableScheduling
@ConditionalOnExpression("!'${app.keep-awake-url:}'.isEmpty()")
public class KeepAwakeJob {

    private static final Logger log = LoggerFactory.getLogger(KeepAwakeJob.class);

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final URI healthUri;

    public KeepAwakeJob(@Value("${app.keep-awake-url}") String baseUrl) {
        this.healthUri = URI.create(baseUrl.replaceAll("/+$", "") + "/health");
    }

    @Scheduled(fixedDelay = 10 * 60_000, initialDelay = 60_000)
    public void ping() {
        try {
            HttpRequest request = HttpRequest.newBuilder(healthUri).timeout(Duration.ofSeconds(30)).GET().build();
            client.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            log.warn("Uyanık tutma isteği başarısız: {}", e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
