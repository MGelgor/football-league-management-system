package com.footballleague.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.footballleague.entity.ManagerSpell;

/** Dönemler menajere göre; profile null olan eski kayıtlar tek kişilik ligin (tek) menajerine aittir. */
public interface ManagerSpellRepository extends JpaRepository<ManagerSpell, Long> {

    @Query("select s from ManagerSpell s where s.profile.id = :profileId or s.profile is null order by s.id")
    List<ManagerSpell> findForProfile(@Param("profileId") Long profileId);

    @Query("""
            select s from ManagerSpell s
            where (s.profile.id = :profileId or s.profile is null) and s.endSeason is null
            order by s.id desc limit 1
            """)
    Optional<ManagerSpell> findOpenSpell(@Param("profileId") Long profileId);
}
