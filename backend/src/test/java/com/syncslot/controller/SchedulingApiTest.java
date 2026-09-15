package com.syncslot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncslot.dto.AppointmentRequest;
import com.syncslot.dto.CascadeApplyRequest;
import com.syncslot.dto.FindSlotsRequest;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end HTTP smoke test of the scheduling REST API using the spec section 18
 * demo scenario. Exercises the Controller -> Service -> Engine -> Repository path.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SchedulingApiTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    UserRepository userRepository;
    @Autowired
    AvailabilityRepository availabilityRepository;
    @Autowired
    AppointmentRepository appointmentRepository;

    private static final LocalDate D = LocalDate.of(2026, 9, 15);
    private User alice, bob, charlie;

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

        appointmentRepository.save(new Appointment("Team Catchup", D,
                LocalTime.of(10, 0), LocalTime.of(11, 0), 60, 0, 0,
                Priority.LOW, AppointmentStatus.CONFIRMED, List.of(alice.getId(), bob.getId())));
    }

    @Test
    void listUsers() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void findSlotsReturnsRankedAlternatives() throws Exception {
        FindSlotsRequest req = new FindSlotsRequest(
                List.of(alice.getId(), bob.getId(), charlie.getId()), D, 60, 0, 0, Priority.HIGH, "Executive Review");
        mockMvc.perform(post("/api/schedule/find-slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots").isNotEmpty());
    }

    @Test
    void previewAndApplyCascadeDemoFlow() throws Exception {
        AppointmentRequest request = new AppointmentRequest("Executive Review",
                List.of(alice.getId(), bob.getId(), charlie.getId()),
                D, LocalTime.of(10, 0), LocalTime.of(11, 0), 60, 0, 0, Priority.HIGH);

        String body = objectMapper.writeValueAsString(request);

        String previewJson = mockMvc.perform(post("/api/schedule/preview-cascade")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Safe to apply"))
                .andExpect(jsonPath("$.impact").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        // Build apply request from the preview impact
        com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(previewJson);
        List<com.syncslot.dto.MoveDto> moves = objectMapper.readerForListOf(com.syncslot.dto.MoveDto.class)
                .readValue(node.get("impact"));
        CascadeApplyRequest apply = new CascadeApplyRequest(request, moves);

        mockMvc.perform(post("/api/schedule/apply-cascade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(apply)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applied").value(true))
                .andExpect(jsonPath("$.appointments").isArray());
    }
}
