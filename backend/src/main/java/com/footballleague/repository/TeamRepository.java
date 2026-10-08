package com.footballleague.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.footballleague.entity.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {

    @EntityGraph(attributePaths = "manager")
    List<Team> findByActiveTrue();

    @EntityGraph(attributePaths = "manager")
    List<Team> findByActiveTrueAndDivision(int division);

    boolean existsByNameIgnoreCaseAndActiveTrue(String name);

    Optional<Team> findByNameIgnoreCaseAndActiveTrue(String name);
}
