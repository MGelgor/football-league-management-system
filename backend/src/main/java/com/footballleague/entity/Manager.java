package com.footballleague.entity;

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

/** Teknik direktör. Taktik ustalığı (1-100) maç hesabında takımın gücüne küçük bir artı / eksi verir. */
@Entity
@Table(name = "managers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Manager {

    public static final int MIN_SKILL = 1;
    public static final int MAX_SKILL = 100;
    // Ustalık 100 → +2.5, 50 → 0, 1 → yaklaşık -2.5 efektif güç
    private static final double SKILL_DIVISOR = 20.0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "tactical_skill", nullable = false)
    private Integer tacticalSkill;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_formation", nullable = false, length = 10)
    @ColumnDefault("'F442'")
    @Builder.Default
    private Formation preferredFormation = Formation.F442;

    public double strengthBonus() {
        return (tacticalSkill - 50) / SKILL_DIVISOR;
    }
}
