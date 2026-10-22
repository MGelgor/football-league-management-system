package com.footballleague.entity;

import java.time.Instant;
import java.time.LocalTime;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Çok oyunculu oda. Odanın ligi (takımlar, maçlar, oyuncular…) ayrı bir veritabanı şemasında durur (schemaName);
 * bu tablo ve room_members varsayılan şemadadır.
 */
@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // Davet kodu: 6 karakter, karıştırılan harfler (0/O, 1/I) yok
    @Column(nullable = false, unique = true, length = 6)
    private String code;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(name = "schema_name", nullable = false, unique = true, length = 30)
    private String schemaName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ColumnDefault("'LOBBY'")
    @Builder.Default
    private RoomStatus status = RoomStatus.LOBBY;

    // Maçların her gün oynandığı saat (timeZone'a göre)
    @Column(name = "match_time", nullable = false)
    @ColumnDefault("'21:00:00'")
    @Builder.Default
    private LocalTime matchTime = DEFAULT_MATCH_TIME;

    @Column(name = "time_zone", nullable = false, length = 40)
    @ColumnDefault("'Europe/Istanbul'")
    @Builder.Default
    private String timeZone = DEFAULT_TIME_ZONE;

    // Transfer penceresinin süresi (saat); herkes hazır derse erken kapanır
    @Column(name = "transfer_hours", nullable = false)
    @ColumnDefault("24")
    @Builder.Default
    private int transferHours = DEFAULT_TRANSFER_HOURS;

    // Sıradaki olayın zamanı (maç günü ya da pencerenin kapanışı); lobide null
    @Column(name = "next_event_at")
    private Instant nextEventAt;

    public static final LocalTime DEFAULT_MATCH_TIME = LocalTime.of(21, 0);
    public static final String DEFAULT_TIME_ZONE = "Europe/Istanbul";
    public static final int DEFAULT_TRANSFER_HOURS = 24;
}
