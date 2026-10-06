package com.footballleague.entity;

import jakarta.persistence.Column;
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

@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_week_id", nullable = false)
    private MatchWeek matchWeek;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "home_team_id", nullable = false)
    private Team homeTeam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "away_team_id", nullable = false)
    private Team awayTeam;

    @Column(name = "home_score")
    private Integer homeScore;

    @Column(name = "away_score")
    private Integer awayScore;

    // Maç oynanırken o anki güç/morale göre hesaplanan maç öncesi olasılıklar (yüzde)
    @Column(name = "home_win_probability")
    private Integer homeWinProbability;

    @Column(name = "draw_probability")
    private Integer drawProbability;

    @Column(name = "away_win_probability")
    private Integer awayWinProbability;

    // Yalnızca beraberlikle biten kupa maçlarında
    @Column(name = "home_penalties")
    private Integer homePenalties;

    @Column(name = "away_penalties")
    private Integer awayPenalties;

    public boolean isPlayed() {
        return homeScore != null && awayScore != null;
    }

    /** Kazanan takım (penaltılar dahil); beraberlikte ve oynanmamışsa null. */
    public Team winner() {
        if (!isPlayed()) {
            return null;
        }
        int home = homeScore * 100 + (homePenalties == null ? 0 : homePenalties);
        int away = awayScore * 100 + (awayPenalties == null ? 0 : awayPenalties);
        if (home == away) {
            return null;
        }
        return home > away ? homeTeam : awayTeam;
    }
}
