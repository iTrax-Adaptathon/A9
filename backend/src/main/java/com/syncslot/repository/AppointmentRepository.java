package com.syncslot.repository;

import com.syncslot.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByDate(LocalDate date);

    List<Appointment> findByDateOrderByStartTimeAsc(LocalDate date);

    List<Appointment> findByDateBetweenOrderByDateAscStartTimeAsc(LocalDate start, LocalDate end);
}
