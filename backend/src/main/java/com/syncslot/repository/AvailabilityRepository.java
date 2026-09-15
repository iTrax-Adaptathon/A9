package com.syncslot.repository;

import com.syncslot.model.Availability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AvailabilityRepository extends JpaRepository<Availability, Long> {

    List<Availability> findByUserId(Long userId);

    List<Availability> findByDate(LocalDate date);

    List<Availability> findByUserIdAndDate(Long userId, LocalDate date);
}
