package com.footballleague.entity;

import java.time.Instant;

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
 * Odada transfer penceresinde verilen kapalı teklif. Teklifler birbirini görmez; pencere kapanınca her oyuncu
 * için geçerli en yüksek teklif kazanır (eşitlikte önce verilen).
 */
@Entity
@Table(name = "transfer_bids")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TransferBid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    // Teklif veren (alıcı) takım
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false)
    private Long fee;

    // Pencerenin açıldığı yeni sezon ("Sezon 3 öncesi")
    @Column(name = "season_number", nullable = false)
    private Integer seasonNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BidStatus status;

    // Sonuç açıklaması (ör. "Daha yüksek teklif kazandı")
    @Column(length = 150)
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
