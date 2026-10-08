package com.footballleague.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.footballleague.dto.LeagueSnapshot;

/**
 * Tüm veritabanını tek bir JSON'a (LeagueSnapshot) çevirir ve geri yükler.
 * Tablolar JDBC metadata'sından bulunur ve yabancı anahtar sırasına göre dizilir; böylece yeni eklenen
 * tablolar da kod değişmeden yedeğe girer. Geri yüklemede tüm tablolar boşaltılır, satırlar kendi id'leriyle
 * eklenir ve id sayaçları en büyük id'nin üstüne alınır.
 */
@Service
public class BackupService {

    static final int FORMAT_VERSION = 1;

    private final JdbcTemplate jdbcTemplate;
    private final Path uploadDir;

    public BackupService(JdbcTemplate jdbcTemplate, @Value("${app.upload-dir}") String uploadDir) {
        this.jdbcTemplate = jdbcTemplate;
        this.uploadDir = Path.of(uploadDir);
    }

    @Transactional(readOnly = true)
    public LeagueSnapshot export() {
        Map<String, LeagueSnapshot.TableData> tables = new LinkedHashMap<>();
        for (String table : tablesInInsertOrder()) {
            tables.put(table, readTable(table));
        }
        return new LeagueSnapshot(FORMAT_VERSION, Instant.now().toString(), summary(), tables,
                readLogos(tables.get("teams")));
    }

    @Transactional
    public void importSnapshot(LeagueSnapshot snapshot) {
        if (snapshot == null || snapshot.tables() == null || snapshot.formatVersion() != FORMAT_VERSION) {
            throw new IllegalArgumentException("Geçersiz yedek dosyası");
        }
        List<String> tables = tablesInInsertOrder();
        Set<String> unknown = new HashSet<>(snapshot.tables().keySet());
        tables.forEach(unknown::remove);
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("Yedekte bu sürümde olmayan tablolar var: " + unknown);
        }

        List<String> deleteOrder = new ArrayList<>(tables);
        Collections.reverse(deleteOrder);
        deleteOrder.forEach(table -> jdbcTemplate.update("delete from " + table));

        for (String table : tables) {
            LeagueSnapshot.TableData data = snapshot.tables().get(table);
            if (data != null && !data.rows().isEmpty()) {
                insertRows(table, data);
            }
            restartIdentity(table);
        }
        writeLogos(snapshot.files());
    }

    /** Kayıt noktası listesi için: takım sayısı, güncel sezon, oynanmış maç sayısı. */
    LeagueSnapshot.Summary summary() {
        Integer teamCount = jdbcTemplate.queryForObject("select count(*) from teams where active = true", Integer.class);
        Integer seasonNumber = jdbcTemplate.queryForObject("select max(season_number) from seasons", Integer.class);
        Long played = jdbcTemplate.queryForObject("select count(*) from matches where home_score is not null", Long.class);
        return new LeagueSnapshot.Summary(teamCount == null ? 0 : teamCount, seasonNumber, played == null ? 0 : played);
    }

    private LeagueSnapshot.TableData readTable(String table) {
        return jdbcTemplate.query("select * from " + table + " order by id", (ResultSet rs) -> {
            ResultSetMetaData meta = rs.getMetaData();
            List<String> columns = new ArrayList<>();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                columns.add(meta.getColumnLabel(i).toLowerCase(Locale.ROOT));
            }
            List<List<Object>> rows = new ArrayList<>();
            while (rs.next()) {
                List<Object> row = new ArrayList<>(columns.size());
                for (int i = 1; i <= columns.size(); i++) {
                    row.add(jsonValue(rs.getObject(i)));
                }
                rows.add(row);
            }
            return new LeagueSnapshot.TableData(columns, rows);
        });
    }

    /** JSON'a sayı, mantıksal değer ve metin olarak yazılır; enum gibi diğer tipler metne çevrilir. */
    private static Object jsonValue(Object value) {
        if (value == null || value instanceof Number || value instanceof Boolean || value instanceof String) {
            return value;
        }
        return value.toString();
    }

    private void insertRows(String table, LeagueSnapshot.TableData data) {
        String sql = "insert into " + table + " (" + String.join(", ", data.columns()) + ") values ("
                + String.join(", ", Collections.nCopies(data.columns().size(), "?")) + ")";
        List<Object[]> batch = data.rows().stream().map(List::toArray).toList();
        try {
            jdbcTemplate.batchUpdate(sql, batch);
        } catch (DataAccessException e) {
            throw new IllegalArgumentException("Yedek bu sürümle uyumsuz (" + table + " tablosu yüklenemedi)", e);
        }
    }

    private void restartIdentity(String table) {
        Long maxId = jdbcTemplate.queryForObject("select max(id) from " + table, Long.class);
        jdbcTemplate.execute("alter table " + table + " alter column id restart with " + ((maxId == null ? 0 : maxId) + 1));
    }

    /** Veritabanındaki tablolar; başka tabloya bağlı olan tablo, bağlı olduğu tablodan sonra gelir. */
    List<String> tablesInInsertOrder() {
        Map<String, Set<String>> dependencies = jdbcTemplate.execute((ConnectionCallback<Map<String, Set<String>>>) this::readDependencies);
        List<String> ordered = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        for (String table : dependencies.keySet()) {
            visit(table, dependencies, visited, ordered);
        }
        return ordered;
    }

    private static void visit(String table, Map<String, Set<String>> dependencies, Set<String> visited,
            List<String> ordered) {
        if (!visited.add(table)) {
            return;
        }
        for (String dependency : dependencies.getOrDefault(table, Set.of())) {
            if (!dependency.equals(table)) {
                visit(dependency, dependencies, visited, ordered);
            }
        }
        ordered.add(table);
    }

    private Map<String, Set<String>> readDependencies(Connection connection) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        String schema = connection.getSchema();
        Map<String, Set<String>> dependencies = new LinkedHashMap<>();
        try (ResultSet tables = meta.getTables(connection.getCatalog(), schema, "%", new String[] {"TABLE"})) {
            while (tables.next()) {
                dependencies.put(tables.getString("TABLE_NAME").toLowerCase(Locale.ROOT), new LinkedHashSet<>());
            }
        }
        for (String table : dependencies.keySet()) {
            for (String name : List.of(table, table.toUpperCase(Locale.ROOT))) {
                try (ResultSet keys = meta.getImportedKeys(connection.getCatalog(), schema, name)) {
                    while (keys.next()) {
                        dependencies.get(table).add(keys.getString("PKTABLE_NAME").toLowerCase(Locale.ROOT));
                    }
                }
            }
        }
        return dependencies;
    }

    private Map<String, String> readLogos(LeagueSnapshot.TableData teams) {
        Map<String, String> files = new HashMap<>();
        int logoColumn = teams == null ? -1 : teams.columns().indexOf("logo_path");
        if (logoColumn < 0) {
            return files;
        }
        for (List<Object> row : teams.rows()) {
            if (row.get(logoColumn) instanceof String path) {
                Path file = uploadDir.resolve(path).normalize();
                if (file.startsWith(uploadDir.normalize()) && Files.isRegularFile(file)) {
                    try {
                        files.put(path, Base64.getEncoder().encodeToString(Files.readAllBytes(file)));
                    } catch (IOException e) {
                        throw new IllegalStateException("Logo okunamadı: " + path, e);
                    }
                }
            }
        }
        return files;
    }

    private void writeLogos(Map<String, String> files) {
        if (files == null) {
            return;
        }
        files.forEach((path, content) -> {
            Path file = uploadDir.resolve(path).normalize();
            // Yedekteki yol yükleme klasörünün dışına çıkamaz
            if (!file.startsWith(uploadDir.normalize().resolve("logos"))) {
                throw new IllegalArgumentException("Geçersiz dosya yolu: " + path);
            }
            try {
                Files.createDirectories(file.getParent());
                Files.write(file, Base64.getDecoder().decode(content));
            } catch (IOException e) {
                throw new IllegalStateException("Logo yazılamadı: " + path, e);
            }
        });
    }
}
