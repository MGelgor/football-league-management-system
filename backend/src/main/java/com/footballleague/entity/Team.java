package com.footballleague.entity;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "teams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Team {

    public static final int INITIAL_MORALE = 50;
    public static final int MIN_STRENGTH = 1;
    public static final int REGULAR_MAX_STRENGTH = 84;
    public static final int BIG_FOUR_MIN_STRENGTH = 85;
    public static final int MAX_STRENGTH = 100;
    // Yalnızca maç hesabında 4 büyüklere eklenir (gösterilen güç değişmez)
    public static final int BIG_FOUR_MATCH_BONUS = 8;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // Arşivlenen (silinen ama maç geçmişi olan) takımlar aynı adı tekrar kullanabilsin diye DB'de unique değil;
    // aktif takımlar arasında tekillik TeamService'te kontrol edilir
    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "founded_year", nullable = false)
    private Integer foundedYear;

    @Column(nullable = false, length = 100)
    private String colors;

    @Column(name = "logo_path")
    private String logoPath;

    @Column(nullable = false)
    private Integer strength;

    @Column(nullable = false)
    private Integer morale;

    @Column(name = "big_four", nullable = false)
    private boolean bigFour;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "season_start_strength")
    private Integer seasonStartStrength;

    @Column(name = "last_strength_change", nullable = false)
    @Builder.Default
    private Integer lastStrengthChange = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @ColumnDefault("'F442'")
    @Builder.Default
    private Formation formation = Formation.F442;

    // Varsayılan stil; yapay zekâ maçtan maça rakibe göre değiştirebilir
    @Enumerated(EnumType.STRING)
    @Column(name = "play_style", nullable = false, length = 20)
    @ColumnDefault("'BALANCED'")
    @Builder.Default
    private PlayStyle playStyle = PlayStyle.BALANCED;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Manager manager;

    // Avro; ekonomi özelliğinden önce oluşturulan takımlarda açılışta doldurulur
    private Long budget;

    // 1 = 1. Lig, 2 = 2. Lig
    @Column(nullable = false)
    @ColumnDefault("1")
    @Builder.Default
    private int division = 1;

    public void addToBudget(long amount) {
        budget = (budget == null ? 0 : budget) + amount;
    }

    public void changeStrength(int delta) {
        int min = bigFour ? BIG_FOUR_MIN_STRENGTH : MIN_STRENGTH;
        int max = bigFour ? MAX_STRENGTH : REGULAR_MAX_STRENGTH;
        int updated = Math.clamp((long) strength + delta, min, max);
        lastStrengthChange = updated - strength;
        strength = updated;
    }

    /** Skor ve olasılık hesabında kullanılan güç. */
    public int matchStrength() {
        return strength + (bigFour ? BIG_FOUR_MATCH_BONUS : 0);
    }

    public int seasonStrengthChange() {
        return seasonStartStrength == null ? 0 : strength - seasonStartStrength;
    }
}
