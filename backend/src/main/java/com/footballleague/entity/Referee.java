package com.footballleague.entity;

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

/** Hakem; sertlik (1-10) kart ve penaltı olasılığını artırır, 5 ortalama. */
@Entity
@Table(name = "referees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Referee {

    public static final int MIN_STRICTNESS = 1;
    public static final int MAX_STRICTNESS = 10;
    public static final int AVERAGE_STRICTNESS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer strictness;

    /** Kart olasılığı çarpanı: sertlik 1 → 0.68, 5 → 1.0, 10 → 1.4. */
    public static double cardFactor(Referee referee) {
        return 0.6 + strictnessOf(referee) * 0.08;
    }

    /** Penaltı olasılığı çarpanı: sertlik 1 → 0.76, 5 → 1.0, 10 → 1.3. */
    public static double penaltyFactor(Referee referee) {
        return 0.7 + strictnessOf(referee) * 0.06;
    }

    private static int strictnessOf(Referee referee) {
        return referee == null ? AVERAGE_STRICTNESS : referee.getStrictness();
    }
}
