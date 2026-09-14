package com.example.scheduler.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<CalendarEntity, UUID> {
}
