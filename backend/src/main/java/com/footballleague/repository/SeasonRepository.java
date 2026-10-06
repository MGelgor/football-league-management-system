package com.footballleague.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.footballleague.entity.Season;

public interface SeasonRepository extends JpaRepository<Season, Long> {

    /** En son sezon = "güncel" sezon (bitmiş de olabilir). */
    Optional<Season> findTopByOrderBySeasonNumberDesc();

    @EntityGraph(attributePaths = {"champion", "cupWinner"})
    List<Season> findAllByOrderBySeasonNumberDesc();
}
