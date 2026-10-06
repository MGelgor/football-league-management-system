package com.footballleague.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.footballleague.entity.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {

    List<Team> findByActiveTrue();

    boolean existsByNameIgnoreCaseAndActiveTrue(String name);

    Optional<Team> findByNameIgnoreCaseAndActiveTrue(String name);
}
