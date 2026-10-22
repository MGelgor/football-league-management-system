package com.footballleague.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.footballleague.entity.ManagerProfile;

public interface ManagerProfileRepository extends JpaRepository<ManagerProfile, Long> {

    Optional<ManagerProfile> findFirstByOrderByIdAsc();

    Optional<ManagerProfile> findByMemberId(Long memberId);

    Optional<ManagerProfile> findByTeamId(Long teamId);

    @Query("select p from ManagerProfile p join fetch p.team where p.team is not null order by p.id")
    List<ManagerProfile> findWithTeam();
}
