package com.footballleague.service;

import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

/**
 * Sezon sonu oyuncu gelişimi: gençler gelişir, yaşlılar geriler; sezonu iyi geçiren (+1) / kötü geçiren (−1)
 * oyuncuya ek değişim. 35 yaşından sonra artan olasılıkla emeklilik.
 */
@Component
public class PlayerDevelopment {

    static final double GOOD_SEASON_RATING = 7.2;
    static final double BAD_SEASON_RATING = 5.8;

    /** age: sezon boyunca oynadığı yaş; averageRating: sezonda hiç oynamadıysa null. */
    public int strengthChange(int age, Double averageRating) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int change;
        if (age <= 21) {
            change = random.nextInt(2, 6);
        } else if (age <= 25) {
            change = random.nextInt(0, 4);
        } else if (age <= 29) {
            change = random.nextInt(-1, 2);
        } else if (age <= 32) {
            change = random.nextInt(-3, 1);
        } else {
            change = random.nextInt(-5, -1);
        }
        if (averageRating != null && averageRating >= GOOD_SEASON_RATING) {
            change++;
        } else if (averageRating != null && averageRating <= BAD_SEASON_RATING) {
            change--;
        }
        return change;
    }

    /** age: yeni (bir yaş büyümüş) yaşı. */
    public boolean retires(int age) {
        double chance;
        if (age >= 38) {
            chance = 1.0;
        } else if (age == 37) {
            chance = 0.8;
        } else if (age == 36) {
            chance = 0.5;
        } else if (age == 35) {
            chance = 0.25;
        } else {
            chance = 0;
        }
        return ThreadLocalRandom.current().nextDouble() < chance;
    }
}
