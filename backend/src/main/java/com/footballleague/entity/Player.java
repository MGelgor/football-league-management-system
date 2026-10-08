package com.footballleague.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.annotations.ColumnDefault;

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
    // Form: son 5 maç reyting ortalamasından 0.9-1.1 arası çarpan
    public static final int FORM_MATCHES = 5;
    private static final double NEUTRAL_RATING = 6.5;
    private static final double FORM_PER_RATING = 0.08;
    private static final double MIN_FORM = 0.9;
    private static final double MAX_FORM = 1.1;
    // Yorgunluk: üst üste bu kadar maçtan sonra her ilk 11 maçı 2 güç düşürür, en fazla 8
    private static final int FRESH_STARTS = 3;
    private static final int FATIGUE_PER_START = 2;
    private static final int MAX_FATIGUE = 8;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // null = serbest oyuncu (sözleşmesi bitip takımsız kaldı)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
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

    @Enumerated(EnumType.STRING)
    @Column(name = "injury_severity", length = 20)
    private InjurySeverity injurySeverity;

    // Son 5 maçın reytingleri, eskiden yeniye virgülle ("6.8,7.2")
    @Column(name = "recent_ratings", nullable = false, length = 40)
    @ColumnDefault("''")
    @Builder.Default
    private String recentRatings = "";

    // Yönetilen takımın altyapısından gelen ve menajerin kararını bekleyen genç (takımı henüz yok)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academy_team_id")
    private Team academyTeam;

    // Haftalık maaş (avro) ve sözleşmenin bittiği sezon (o sezonun sonunda biter); eski oyuncularda açılışta dolar
    private Long wage;

    @Column(name = "contract_until")
    private Integer contractUntil;

    // Üst üste ilk 11'de başladığı maç sayısı (yorgunluk); yedek kalınca sıfırlanır
    @Column(name = "consecutive_starts", nullable = false)
    @ColumnDefault("0")
    @Builder.Default
    private int consecutiveStarts = 0;

    // false = emekli; geçmiş maç kayıtları bozulmasın diye silinmez
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    public boolean isAvailable() {
        return active && suspendedMatches == 0 && injuredMatches == 0;
    }

    public List<Double> ratingHistory() {
        if (recentRatings == null || recentRatings.isBlank()) {
            return List.of();
        }
        return Arrays.stream(recentRatings.split(",")).map(Double::valueOf).toList();
    }

    public void addRating(double rating) {
        List<Double> ratings = new ArrayList<>(ratingHistory());
        ratings.add(rating);
        recentRatings = String.join(",", ratings.subList(Math.max(0, ratings.size() - FORM_MATCHES), ratings.size())
                .stream().map(String::valueOf).toList());
    }

    /** 1.0 = nötr; hiç maçı yoksa 1.0. */
    public double form() {
        List<Double> ratings = ratingHistory();
        if (ratings.isEmpty()) {
            return 1.0;
        }
        double average = ratings.stream().mapToDouble(Double::doubleValue).average().orElse(NEUTRAL_RATING);
        return Math.clamp(1 + (average - NEUTRAL_RATING) * FORM_PER_RATING, MIN_FORM, MAX_FORM);
    }

    public int fatigue() {
        return Math.min(MAX_FATIGUE, Math.max(0, consecutiveStarts - FRESH_STARTS) * FATIGUE_PER_START);
    }

    /** İlk 11 seçiminde ve gol / asist ağırlığında kullanılan güç: güç × form − yorgunluk. */
    public double effectiveStrength() {
        return Math.max(1, strength * form() - fatigue());
    }

    public void changeStrength(int delta) {
        int updated = Math.clamp((long) strength + delta, MIN_STRENGTH, MAX_STRENGTH);
        lastStrengthChange = updated - strength;
        strength = updated;
    }
}
