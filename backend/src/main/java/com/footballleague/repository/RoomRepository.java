package com.footballleague.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.footballleague.entity.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByCode(String code);

    boolean existsByCode(String code);

    List<Room> findByNextEventAtLessThanEqual(Instant time);

    @Query("select min(r.nextEventAt) from Room r")
    Optional<Instant> findEarliestNextEventAt();
}
