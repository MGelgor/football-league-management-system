package com.footballleague.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Bir takımın bir maçtaki istatistikleri. Her oynanmış maç için iki satır (ev sahibi + deplasman).
 * Kart sayıları burada tutulmaz, MatchEvent'lerden sayılır.
 */
@Entity
@Table(name = "match_team_stats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MatchTeamStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false)
    private boolean home;

    @Column(nullable = false)
    private Integer possession;

    @Column(nullable = false)
    private Integer shots;

    @Column(name = "shots_on_target", nullable = false)
    private Integer shotsOnTarget;

    @Column(nullable = false)
    private Integer corners;

    @Column(nullable = false)
    private Integer fouls;

    @Column(nullable = false)
    private Integer offsides;

    @Column(nullable = false)
    private Integer saves;

    // Maçtan sonraki takım gücü (güç geçmişi grafiği için)
    @Column(name = "strength_after")
    private Integer strengthAfter;

    // Maçta oynanan diziliş ve stil (bu özellikten önceki maçlarda null)
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Formation formation;

    @Enumerated(EnumType.STRING)
    @Column(name = "play_style", length = 20)
    private PlayStyle playStyle;
}
