package com.footballleague.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.footballleague.entity.Manager;

public interface ManagerRepository extends JpaRepository<Manager, Long> {
}
