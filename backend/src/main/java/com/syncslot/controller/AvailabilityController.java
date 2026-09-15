package com.syncslot.controller;

import com.syncslot.dto.AvailabilityDto;
import com.syncslot.model.Availability;
import com.syncslot.repository.AvailabilityRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/availability")
public class AvailabilityController {

    private final AvailabilityRepository availabilityRepository;

    public AvailabilityController(AvailabilityRepository availabilityRepository) {
        this.availabilityRepository = availabilityRepository;
    }

    @GetMapping
    public List<AvailabilityDto> list(@RequestParam(required = false) Long userId,
                                      @RequestParam(required = false) LocalDate date) {
        List<Availability> result;
        if (userId != null && date != null) {
            result = availabilityRepository.findByUserIdAndDate(userId, date);
        } else if (userId != null) {
            result = availabilityRepository.findByUserId(userId);
        } else if (date != null) {
            result = availabilityRepository.findByDate(date);
        } else {
            result = availabilityRepository.findAll();
        }
        return result.stream().map(this::toDto).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AvailabilityDto create(@RequestBody AvailabilityDto dto) {
        validate(dto);
        Availability a = new Availability(dto.userId(), dto.date(), dto.startTime(), dto.endTime());
        return toDto(availabilityRepository.save(a));
    }

    @PutMapping("/{id}")
    public AvailabilityDto update(@PathVariable Long id, @RequestBody AvailabilityDto dto) {
        validate(dto);
        Availability a = availabilityRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Availability not found"));
        a.setUserId(dto.userId());
        a.setDate(dto.date());
        a.setStartTime(dto.startTime());
        a.setEndTime(dto.endTime());
        return toDto(availabilityRepository.save(a));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!availabilityRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Availability not found");
        }
        availabilityRepository.deleteById(id);
    }

    private void validate(AvailabilityDto dto) {
        if (dto.userId() == null || dto.date() == null
                || dto.startTime() == null || dto.endTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId, date, startTime and endTime are required");
        }
        if (!dto.endTime().isAfter(dto.startTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endTime must be after startTime");
        }
    }

    private AvailabilityDto toDto(Availability a) {
        return new AvailabilityDto(a.getId(), a.getUserId(), a.getDate(), a.getStartTime(), a.getEndTime());
    }
}
