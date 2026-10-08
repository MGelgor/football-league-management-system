package com.footballleague.dto;

import com.footballleague.entity.InjurySeverity;
import com.footballleague.entity.Position;

/**
 * Kadrodaki oyuncu. Sezon alanları (appearances..playerOfTheMatch) güncel sezonun lig maçlarına,
 * career* alanları tüm sezon ve turnuvalara aittir. averageRating hiç oynamadıysa null.
 */
public record PlayerResponse(
        Long id,
        String name,
        Position position,
        Integer shirtNumber,
        Integer strength,
        Integer age,
        int lastStrengthChange,
        int suspendedMatches,
        int injuredMatches,
        int appearances,
        int minutes,
        int goals,
        int assists,
        int yellowCards,
        int redCards,
        Double averageRating,
        int playerOfTheMatch,
        int careerAppearances,
        int careerGoals,
        int careerAssists,
        // Form çarpanı 0.9-1.1, yorgunluk (efektif güçten düşülen), sakatlık türü
        double form,
        int fatigue,
        InjurySeverity injurySeverity,
        // Avro; haftalık maaş ve sözleşmenin bittiği sezon
        long marketValue,
        Long wage,
        Integer contractUntil
) {
}
