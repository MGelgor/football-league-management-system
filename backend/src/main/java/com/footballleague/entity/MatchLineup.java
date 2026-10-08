package com.footballleague.entity;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Kullanıcının bir maç için seçtiği kadro: diziliş, stil, ilk 11, kaptan, penaltıcı. */
@Entity
@Table(name = "match_lineups", uniqueConstraints = @UniqueConstraint(columnNames = {"match_id", "team_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MatchLineup {

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Formation formation;

    @Enumerated(EnumType.STRING)
    @Column(name = "play_style", nullable = false, length = 20)
    private PlayStyle playStyle;

    // İlk 11 oyuncu id'leri, virgülle
    @Column(name = "starter_ids", nullable = false, length = 200)
    private String starterIds;

    @Column(name = "captain_id")
    private Long captainId;

    @Column(name = "penalty_taker_id")
    private Long penaltyTakerId;

    public List<Long> starterIdList() {
        return Arrays.stream(starterIds.split(",")).map(Long::valueOf).toList();
    }

    public void setStarterIdList(List<Long> ids) {
        starterIds = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}
