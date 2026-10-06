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

/**
 * Bir oyuncunun bir maçta sahada bulunduğu süre (ilk 11 ya da sonradan giren).
 * Oynanan maç / dakika, reyting ve maçın oyuncusu buradan hesaplanır.
 */
@Entity
@Table(name = "match_appearances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MatchAppearance {

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(nullable = false)
    private boolean starter;

    // 0 = ilk 11; oyundan çıkış (değişiklik, sakatlık, kırmızı kart) yoksa 90
    @Column(name = "minute_on", nullable = false)
    private Integer minuteOn;

    @Column(name = "minute_off", nullable = false)
    private Integer minuteOff;

    // Yalnızca sonradan girenlerde: yerine girdiği oyuncu
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaced_player_id")
    private Player replacedPlayer;

    // 3.0 - 10.0
    @Column(nullable = false)
    private Double rating;

    @Column(name = "player_of_the_match", nullable = false)
    private boolean playerOfTheMatch;

    public int minutesPlayed() {
        return minuteOff - minuteOn;
    }
}
