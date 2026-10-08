package com.footballleague.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.footballleague.entity.ManagerSpell;

public interface ManagerSpellRepository extends JpaRepository<ManagerSpell, Long> {

    List<ManagerSpell> findAllByOrderByIdAsc();

    Optional<ManagerSpell> findFirstByEndSeasonIsNullOrderByIdDesc();
}
