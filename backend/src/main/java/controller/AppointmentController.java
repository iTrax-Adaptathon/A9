package com.syncslot.backend.controller;

import com.syncslot.backend.model.Appointment;
import com.syncslot.backend.repository.AppointmentRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@CrossOrigin(origins = "*")
public class AppointmentController {

    private final AppointmentRepository repository;

    public AppointmentController(AppointmentRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Appointment> getAllAppointments() {
        return repository.findAll();
    }

    @PostMapping
    public Appointment createAppointment(@RequestBody Appointment appointment) {
        return repository.save(appointment);
    }

    @GetMapping("/{id}")
    public Appointment getAppointment(@PathVariable Long id) {
        return repository.findById(id).orElse(null);
    }

    @DeleteMapping("/{id}")
    public void deleteAppointment(@PathVariable Long id) {
        repository.deleteById(id);
    }
}
