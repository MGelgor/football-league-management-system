package com.footballleague.controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.footballleague.dto.LeagueSnapshot;
import com.footballleague.dto.SaveSlotRequest;
import com.footballleague.dto.SaveSlotResponse;
import com.footballleague.service.BackupService;
import com.footballleague.service.CsvExportService;
import com.footballleague.service.SaveSlotService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
@Tag(name = "Data", description = "JSON yedek / geri yükleme, kayıt noktaları ve CSV dışa aktarma")
public class DataController {

    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final BackupService backupService;
    private final SaveSlotService saveSlotService;
    private final CsvExportService csvExportService;
    private final ObjectMapper objectMapper;

    @GetMapping("/export")
    public ResponseEntity<LeagueSnapshot> export() {
        return attachment("futbol-ligi-yedek-" + LocalDate.now() + ".json", MediaType.APPLICATION_JSON,
                backupService.export());
    }

    /** Mevcut tüm veriyi siler ve yedektekiyle değiştirir. */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void importBackup(@RequestParam("file") MultipartFile file) throws IOException {
        LeagueSnapshot snapshot;
        try (InputStream in = file.getInputStream()) {
            snapshot = objectMapper.readValue(in, LeagueSnapshot.class);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Dosya geçerli bir lig yedeği değil");
        }
        backupService.importSnapshot(snapshot);
    }

    @GetMapping("/saves")
    public List<SaveSlotResponse> listSaves() {
        return saveSlotService.list();
    }

    @PostMapping("/saves")
    @ResponseStatus(HttpStatus.CREATED)
    public SaveSlotResponse save(@Valid @RequestBody SaveSlotRequest request) {
        return saveSlotService.save(request.name());
    }

    @PostMapping("/saves/{name}/load")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void load(@PathVariable String name) {
        saveSlotService.load(name);
    }

    @DeleteMapping("/saves/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSave(@PathVariable String name) {
        saveSlotService.delete(name);
    }

    @GetMapping("/csv/standings")
    public ResponseEntity<String> standingsCsv(@RequestParam(required = false) Long seasonId) {
        return attachment("puan-durumu.csv", CSV, csvExportService.standings(seasonId));
    }

    @GetMapping("/csv/fixture")
    public ResponseEntity<String> fixtureCsv(@RequestParam(required = false) Long seasonId) {
        return attachment("fikstur.csv", CSV, csvExportService.fixture(seasonId));
    }

    @GetMapping("/csv/players")
    public ResponseEntity<String> playersCsv(@RequestParam(required = false) Long seasonId) {
        return attachment("oyuncu-istatistikleri.csv", CSV, csvExportService.players(seasonId));
    }

    private static <T> ResponseEntity<T> attachment(String fileName, MediaType type, T body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build().toString())
                .contentType(type)
                .body(body);
    }
}
