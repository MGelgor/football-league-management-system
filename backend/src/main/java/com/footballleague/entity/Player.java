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

@Entity
@Table(name = "players")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Player {

    public static final int MIN_STRENGTH = 1;
    public static final int MAX_STRENGTH = 100;
    public static final int MIN_AGE = 16;
    public static final int MAX_AGE = 45;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Position position;

    @Column(name = "shirt_number", nullable = false)
    private Integer shirtNumber;

    // 1-100; mevkiyle birlikte golün / asistin bu oyuncuya yazılma olasılığını belirler
    @Column(nullable = false)
    private Integer strength;

    @Column(nullable = false)
    private Integer age;

    // Sezon sonu gelişimiyle gelen son güç değişimi (▲▼)
    @Column(name = "last_strength_change", nullable = false)
    @Builder.Default
    private int lastStrengthChange = 0;

    // Kalan ceza / sakatlık maç sayısı; 0'dan büyükse oyuncu kadroya alınmaz
    @Column(name = "suspended_matches", nullable = false)
    @Builder.Default
    private int suspendedMatches = 0;

    @Column(name = "injured_matches", nullable = false)
    @Builder.Default
    private int injuredMatches = 0;

    // Sarı kart cezası için (her 4 sarıda 1 maç); sezon başında sıfırlanır
    @Column(name = "season_yellow_cards", nullable = false)
    @Builder.Default
    private int seasonYellowCards = 0;

    // false = emekli; geçmiş maç kayıtları bozulmasın diye silinmez
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    public boolean isAvailable() {
        return active && suspendedMatches == 0 && injuredMatches == 0;
    }

    public void changeStrength(int delta) {
        int updated = Math.clamp((long) strength + delta, MIN_STRENGTH, MAX_STRENGTH);
        lastStrengthChange = updated - strength;
        strength = updated;
    }
}
