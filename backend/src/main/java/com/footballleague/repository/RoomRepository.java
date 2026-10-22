package com.footballleague.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.footballleague.entity.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByCode(String code);

    boolean existsByCode(String code);
}
