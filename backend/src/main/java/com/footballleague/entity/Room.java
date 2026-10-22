package com.footballleague.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
}
