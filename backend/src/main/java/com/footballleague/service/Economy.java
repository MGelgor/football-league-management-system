package com.footballleague.service;

import com.footballleague.entity.Player;
import com.footballleague.entity.Position;
import com.footballleague.entity.Team;

/**
 * Ekonomi formülleri (avro). Değer ve gelir güçle üstel büyür: güç 60 bir oyuncu ~1M, 72 ~2.7M, 90 ~12M.
 * Haftalık maaş piyasa değerinin binde 4'ü; lig haftası başına bir kez ödenir.
 */
public final class Economy {

    private static final double BASE_VALUE = 1_000_000;
    private static final double VALUE_STRENGTH_SCALE = 12.0;
    static final double WEEKLY_WAGE_RATE = 0.004;
    private static final double BASE_TICKET = 400_000;
    private static final double TICKET_STRENGTH_SCALE = 15.0;
    private static final double BIG_FOUR_TICKET_BONUS = 1.3;
    // Başlangıç bütçesi = bu kadar iç saha maçı geliri
    private static final int INITIAL_BUDGET_MATCHES = 20;
    static final long CHAMPION_PRIZE = 20_000_000;
    static final long LAST_PLACE_PRIZE = 2_000_000;
    static final double FREE_AGENT_WAGE_BONUS = 1.15;

    private Economy() {
    }

    /** 10 000'e yuvarlanmış piyasa değeri: güç, yaş (gençler daha değerli) ve mevkiye göre. */
    public static long marketValue(int strength, int age, Position position) {
        double value = BASE_VALUE * Math.exp((strength - 60) / VALUE_STRENGTH_SCALE) * ageFactor(age)
                * positionFactor(position);
        return Math.max(10_000, Math.round(value / 10_000) * 10_000);
    }

    public static long marketValue(Player player) {
        return marketValue(player.getStrength(), player.getAge(), player.getPosition());
    }

    /** 100'e yuvarlanmış haftalık maaş. */
    public static long weeklyWage(long marketValue) {
        return Math.max(500, Math.round(marketValue * WEEKLY_WAGE_RATE / 100) * 100);
    }

    /** Transferde imzalanan maaş: değere göre; serbest oyuncu imza için %15 fazla ister. */
    public static long transferWage(long marketValue, boolean freeAgent) {
        long wage = weeklyWage(marketValue);
        return freeAgent ? Math.round(wage * FREE_AGENT_WAGE_BONUS / 100) * 100 : wage;
    }

    /** "€12,5M", "€850B" gibi kısa gösterim (mesajlar için). */
    public static String formatMoney(long amount) {
        long value = Math.abs(amount);
        String sign = amount < 0 ? "-" : "";
        if (value >= 1_000_000) {
            return sign + "€" + String.format(java.util.Locale.forLanguageTag("tr"), "%.1f", value / 1_000_000.0)
                    .replace(",0", "") + "M";
        }
        return sign + "€" + Math.round(value / 1000.0) + "B";
    }

    /** İç saha maçı bilet geliri (1000'e yuvarlanmış). */
    public static long ticketIncome(Team team) {
        double income = BASE_TICKET * Math.exp((team.getStrength() - 60) / TICKET_STRENGTH_SCALE)
                * (team.isBigFour() ? BIG_FOUR_TICKET_BONUS : 1);
        return Math.round(income / 1000) * 1000;
    }

    public static long initialBudget(Team team) {
        return ticketIncome(team) * INITIAL_BUDGET_MATCHES;
    }

    /** Lig sırasına göre ödül: 1. 20M, sonuncu 2M, arası doğrusal. */
    public static long leaguePrize(int rank, int teamCount) {
        if (teamCount <= 1) {
            return CHAMPION_PRIZE;
        }
        return LAST_PLACE_PRIZE + (CHAMPION_PRIZE - LAST_PLACE_PRIZE) * (teamCount - rank) / (teamCount - 1);
    }

    private static double ageFactor(int age) {
        if (age <= 21) {
            return 1.3;
        }
        if (age <= 25) {
            return 1.2;
        }
        if (age <= 29) {
            return 1.0;
        }
        return age <= 32 ? 0.7 : 0.4;
    }

    private static double positionFactor(Position position) {
        return switch (position) {
            case FORWARD -> 1.2;
            case MIDFIELDER -> 1.1;
            case DEFENDER -> 0.9;
            case GOALKEEPER -> 0.7;
        };
    }
}
