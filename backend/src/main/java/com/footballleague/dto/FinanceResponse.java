package com.footballleague.dto;

import java.util.List;

import com.footballleague.entity.FinanceType;
import com.footballleague.entity.Position;

/**
 * Takım finansı: güncel bütçe, seçilen sezonun gelir / giderleri (türe göre), haftalık maaş yükü, kadro değeri,
 * sözleşmeler (en erken biten başta) ve son kayıtlar. Tutarlar avro.
 */
public record FinanceResponse(
        Long teamId,
        long budget,
        Integer seasonNumber,
        long income,
        long expenses,
        List<TypeTotal> totals,
        long weeklyWageBill,
        long squadValue,
        List<ContractLine> contracts,
        List<EntryLine> recentEntries
) {

    public record TypeTotal(FinanceType type, long amount) {
    }

    public record ContractLine(Long playerId, String name, Position position, int age, int strength, long marketValue,
            long wage, Integer contractUntil) {
    }

    public record EntryLine(Integer weekNumber, FinanceType type, long amount, String description) {
    }
}
