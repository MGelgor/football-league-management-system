package com.footballleague.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.footballleague.entity.Player;

/** "Active" sorgular emekli oyuncuları dışarıda bırakır. */
public interface PlayerRepository extends JpaRepository<Player, Long> {

    List<Player> findByTeamIdAndActiveTrueOrderByShirtNumber(Long teamId);

    List<Player> findByTeamIdInAndActiveTrue(Collection<Long> teamIds);

    boolean existsByTeamIdAndActiveTrue(Long teamId);

    boolean existsByTeamIdAndShirtNumberAndActiveTrueAndIdNot(Long teamId, Integer shirtNumber, Long id);

    void deleteByTeamId(Long teamId);
}
