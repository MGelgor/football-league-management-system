package com.footballleague.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.footballleague.entity.Referee;

public interface RefereeRepository extends JpaRepository<Referee, Long> {

    /** Hakem başına oynanmış maç sayısı: [hakem id, maç sayısı]. */
    @Query("""
            select m.referee.id, count(m) from Match m
            where m.referee is not null and m.homeScore is not null
            group by m.referee.id
            """)
    List<Object[]> countPlayedMatches();

    /** Hakem başına olay türü sayıları: [hakem id, olay türü, penaltı mı, adet]. */
    @Query("""
            select e.match.referee.id, e.type, e.penalty, count(e) from MatchEvent e
            where e.match.referee is not null
            group by e.match.referee.id, e.type, e.penalty
            """)
    List<Object[]> countEvents();
}
