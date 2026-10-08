package com.footballleague.dto;

import com.footballleague.entity.Position;

public record AcademyPlayerResponse(Long id, String name, Position position, int age, int strength) {
}
