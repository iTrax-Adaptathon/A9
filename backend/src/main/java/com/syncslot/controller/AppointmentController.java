package com.syncslot.controller;

import com.syncslot.dto.AppointmentDto;
import com.syncslot.dto.AppointmentRequest;
import com.syncslot.model.Appointment;
import com.syncslot.model.AppointmentStatus;
import com.syncslot.model.Priority;
import com.syncslot.repository.AppointmentRepository;
import com.syncslot.util.TimeUtil;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/**
 * Plain CRUD for appointments. Smart conflict handling lives in the scheduling
 * endpoints (/api/schedule/*); the frontend uses those for booking and this
 * controller for simple listing/editing/deleting.
 */
@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentRepository appointmentRepository;

    public AppointmentController(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    @GetMapping
    public List<AppointmentDto> list(@RequestParam(required = false) LocalDate date) {
        List<Appointment> result = (date != null)
                ? appointmentRepository.findByDateOrderByStartTimeAsc(date)
                : appointmentRepository.findAll();
        return result.stream().map(this::toDto).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentDto create(@RequestBody AppointmentRequest req) {
        Appointment a = fromRequest(req);
        a.setStatus(AppointmentStatus.CONFIRMED);
        return toDto(appointmentRepository.save(a));
    }

    @PutMapping("/{id}")
    public AppointmentDto update(@PathVariable Long id, @RequestBody AppointmentRequest req) {
        Appointment a = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        a.setTitle(req.title());
        a.setDate(req.date());
        a.setStartTime(req.startTime());
        a.setEndTime(req.endTime());
        a.setDuration(req.duration() == null
                ? TimeUtil.durationBetween(req.startTime(), req.endTime()) : req.duration());
        a.setBufferBefore(req.bufferBefore() == null ? 0 : req.bufferBefore());
        a.setBufferAfter(req.bufferAfter() == null ? 0 : req.bufferAfter());
        a.setPriority(req.priority() == null ? Priority.MEDIUM : req.priority());
        a.setParticipantIds(req.participantIds());
        return toDto(appointmentRepository.save(a));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!appointmentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found");
        }
        appointmentRepository.deleteById(id);
    }

    private Appointment fromRequest(AppointmentRequest req) {
        if (req.title() == null || req.date() == null || req.startTime() == null || req.endTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title, date, startTime and endTime are required");
        }
        int duration = req.duration() == null
                ? TimeUtil.durationBetween(req.startTime(), req.endTime()) : req.duration();
        return new Appointment(
                req.title(), req.date(), req.startTime(), req.endTime(), duration,
                req.bufferBefore() == null ? 0 : req.bufferBefore(),
                req.bufferAfter() == null ? 0 : req.bufferAfter(),
                req.priority() == null ? Priority.MEDIUM : req.priority(),
                AppointmentStatus.CONFIRMED,
                req.participantIds());
    }

    private AppointmentDto toDto(Appointment a) {
        return new AppointmentDto(a.getId(), a.getTitle(), a.getDate(),
                a.getStartTime(), a.getEndTime(), a.getDuration(),
                a.getBufferBefore(), a.getBufferAfter(), a.getPriority(),
                a.getStatus().name(), a.getParticipantIds());
    }
}
