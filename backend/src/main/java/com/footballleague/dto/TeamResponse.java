package com.footballleague.dto;

import com.footballleague.entity.Formation;
import com.footballleague.entity.PlayStyle;

public record TeamResponse(
        Long id,
        String name,
        Integer foundedYear,
        String colors,
        String logoUrl,
        Integer strength,
        Integer morale,
        boolean bigFour,
        int lastStrengthChange,
        int seasonStrengthChange,
        // false: küme düştü ya da silindi (arşivde)
        boolean active,
        Formation formation,
        PlayStyle playStyle,
        // Avro
        Long budget,
        // 1 = 1. Lig, 2 = 2. Lig
        int division,
        // Teknik direktörü olmayan (eski) takımlarda null
        ManagerRef manager
) {

    public record ManagerRef(Long id, String name, int tacticalSkill, Formation preferredFormation) {
    }
}
