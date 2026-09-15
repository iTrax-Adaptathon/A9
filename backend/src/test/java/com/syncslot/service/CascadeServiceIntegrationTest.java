package com.syncslot.service;

import com.syncslot.dto.AppointmentRequest;
import com.syncslot.dto.CascadeApplyRequest;
import com.syncslot.dto.CascadeApplyResponse;
import com.syncslot.dto.CascadePreviewDto;
import com.syncslot.model.Appointment;
import com.syncslot.model.AppointmentStatus;
import com.syncslot.model.Availability;
import com.syncslot.model.Priority;
import com.syncslot.model.User;
import com.syncslot.repository.AppointmentRepository;
import com.syncslot.repository.AvailabilityRepository;
import com.syncslot.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the cascade workflow (spec section 14 scenarios J and K):
 * preview must be a pure dry-run and apply must persist all intended changes.
 */
@SpringBootTest
class CascadeServiceIntegrationTest {

    @Autowired
    CascadeService cascadeService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    AvailabilityRepository availabilityRepository;
    @Autowired
    AppointmentRepository appointmentRepository;

    private static final LocalDate D = LocalDate.of(2026, 9, 15);

    private User alice;
    private User bob;
    private User charlie;

    @BeforeEach
    void setUp() {
        appointmentRepository.deleteAll();
        availabilityRepository.deleteAll();
        userRepository.deleteAll();

        alice = userRepository.save(new User("Alice", null));
        bob = userRepository.save(new User("Bob", null));
        charlie = userRepository.save(new User("Charlie", null));

        availabilityRepository.save(new Availability(alice.getId(), D, LocalTime.of(9, 0), LocalTime.of(14, 0)));
        availabilityRepository.save(new Availability(bob.getId(), D, LocalTime.of(10, 0), LocalTime.of(15, 0)));
        availabilityRepository.save(new Availability(charlie.getId(), D, LocalTime.of(10, 0), LocalTime.of(12, 0)));
    }

    private Appointment teamCatchup() {
        return appointmentRepository.save(new Appointment("Team Catchup", D,
                LocalTime.of(10, 0), LocalTime.of(11, 0), 60, 0, 0,
                Priority.LOW, AppointmentStatus.CONFIRMED, List.of(alice.getId(), bob.getId())));
    }

    private AppointmentRequest executiveReview() {
        return new AppointmentRequest("Executive Review",
                List.of(alice.getId(), bob.getId(), charlie.getId()),
                D, LocalTime.of(10, 0), LocalTime.of(11, 0), 60, 0, 0, Priority.HIGH);
    }

    // ---- J. preview-cascade does not mutate any data --------------------

    @Test
    void previewCascadeDoesNotMutateDatabase() {
        Appointment team = teamCatchup();
        long countBefore = appointmentRepository.count();
        LocalTime teamStartBefore = team.getStartTime();
        LocalTime teamEndBefore = team.getEndTime();

        CascadePreviewDto preview = cascadeService.previewCascade(executiveReview());

        assertEquals("Safe to apply", preview.status());
        assertFalse(preview.impact().isEmpty(), "Team Catchup should appear in the impact list");

        // DB must be byte-for-byte identical
        assertEquals(countBefore, appointmentRepository.count(), "no new appointment created");
        Appointment teamAfter = appointmentRepository.findById(team.getId()).orElseThrow();
        assertEquals(teamStartBefore, teamAfter.getStartTime(), "Team Catchup start must not change");
        assertEquals(teamEndBefore, teamAfter.getEndTime(), "Team Catchup end must not change");
        assertEquals(AppointmentStatus.CONFIRMED, teamAfter.getStatus());
    }

    // ---- K. apply-cascade persists all intended changes -----------------

    @Test
    void applyCascadePersistsAllChanges() {
        Appointment team = teamCatchup();
        AppointmentRequest request = executiveReview();

        CascadePreviewDto preview = cascadeService.previewCascade(request);
        assertEquals("Safe to apply", preview.status());

        CascadeApplyResponse response = cascadeService.applyCascade(
                new CascadeApplyRequest(request, preview.impact()));

        assertTrue(response.applied(), "apply should succeed");
        List<Appointment> all = appointmentRepository.findByDate(D);

        // Executive Review created at the protected 10:00-11:00 slot
        Appointment executive = all.stream()
                .filter(a -> a.getTitle().equals("Executive Review")).findFirst().orElseThrow();
        assertEquals(LocalTime.of(10, 0), executive.getStartTime());
        assertEquals(LocalTime.of(11, 0), executive.getEndTime());
        assertEquals(3, executive.getParticipantIds().size());

        // Team Catchup moved to a different valid slot and marked rescheduled
        Appointment teamAfter = appointmentRepository.findById(team.getId()).orElseThrow();
        assertNotEquals(LocalTime.of(10, 0), teamAfter.getStartTime(), "Team Catchup must be moved");
        assertEquals(AppointmentStatus.RESCHEDULED, teamAfter.getStatus());
    }

    // ---- apply-cascade with no preview approval still needs a safe plan --

    @Test
    void applyCascadeRefusesUnsafePlan() {
        // Make Team Catchup unable to move: Bob available only 10:00-11:00.
        availabilityRepository.deleteAll();
        availabilityRepository.save(new Availability(alice.getId(), D, LocalTime.of(9, 0), LocalTime.of(14, 0)));
        availabilityRepository.save(new Availability(bob.getId(), D, LocalTime.of(10, 0), LocalTime.of(11, 0)));
        availabilityRepository.save(new Availability(charlie.getId(), D, LocalTime.of(10, 0), LocalTime.of(11, 0)));
        Appointment team = teamCatchup();

        AppointmentRequest request = executiveReview();
        CascadeApplyResponse response = cascadeService.applyCascade(new CascadeApplyRequest(request, List.of()));

        assertFalse(response.applied(), "unsafe plan must not be applied");
        assertEquals(1, appointmentRepository.count(), "no partial writes allowed");
        assertEquals(LocalTime.of(10, 0),
                appointmentRepository.findById(team.getId()).orElseThrow().getStartTime());
    }
}
