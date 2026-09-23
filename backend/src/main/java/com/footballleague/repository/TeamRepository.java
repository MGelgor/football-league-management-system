package com.footballleague.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.footballleague.entity.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {

    boolean existsByNameIgnoreCase(String name);

    Optional<Team> findByNameIgnoreCase(String name);
}
