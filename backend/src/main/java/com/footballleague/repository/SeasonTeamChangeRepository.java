package com.footballleague.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.footballleague.entity.SeasonTeamChange;

public interface SeasonTeamChangeRepository extends JpaRepository<SeasonTeamChange, Long> {

    @Query("select c from SeasonTeamChange c join fetch c.team join fetch c.season order by c.id asc")
    List<SeasonTeamChange> findAllWithTeams();
}
