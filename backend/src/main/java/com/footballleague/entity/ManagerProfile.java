package com.footballleague.entity;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * "Takımımı Yönet" modu: tek kullanıcının menajer profili (hesap yok, tabloda en fazla bir satır).
 * team null ise mod kapalıdır (ya da menajer işsizdir).
 */
@Entity
@Table(name = "manager_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ManagerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    // Takımı devraldığı sezon (o sırada sezon yoksa 1)
    @Column(name = "started_season", nullable = false)
    private Integer startedSeason;

    // Yönetim kurulunun güveni 0-100; 0'a düşerse menajer kovulur
    @Column(nullable = false)
    @ColumnDefault("60")
    @Builder.Default
    private int confidence = 60;

    // Sezon hedefleri: lig sırası (en kötü), kupada ulaşılacak tur (null = hedef yok), hedeflerin sezonu
    @Column(name = "target_rank")
    private Integer targetRank;

    @Enumerated(EnumType.STRING)
    @Column(name = "cup_target", length = 20)
    private CupRound cupTarget;

    @Column(name = "objective_season")
    private Integer objectiveSeason;
}
