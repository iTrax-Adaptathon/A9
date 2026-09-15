package com.syncslot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncslot.dto.AiSummaryRequest;
import com.syncslot.model.Appointment;
import com.syncslot.model.AppointmentStatus;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the AI summary endpoint counts meetings for the requested range and
 * degrades to a deterministic summary when no API key is configured.
 */
@SpringBootTest(properties = "app.ai.api-key=")
@AutoConfigureMockMvc
class AiSummaryApiTest {

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

    private static final LocalDate TUE = LocalDate.of(2026, 9, 15);

    @BeforeEach
    void setUp() {
        appointmentRepository.deleteAll();
        availabilityRepository.deleteAll();
        userRepository.deleteAll();

        User alice = userRepository.save(new User("Alice", null));
        User bob = userRepository.save(new User("Bob", null));

        appointmentRepository.save(new Appointment("Executive Review", TUE,
                LocalTime.of(10, 0), LocalTime.of(11, 0), 60, 0, 0,
                Priority.URGENT, AppointmentStatus.CONFIRMED, List.of(alice.getId(), bob.getId())));
        appointmentRepository.save(new Appointment("Team Catchup", TUE,
                LocalTime.of(13, 0), LocalTime.of(14, 0), 60, 0, 0,
                Priority.LOW, AppointmentStatus.CONFIRMED, List.of(alice.getId())));
    }

    private void expectSummary(LocalDate date, String range, int count) throws Exception {
        AiSummaryRequest request = new AiSummaryRequest(date, range);
        mockMvc.perform(post("/api/ai/summary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiGenerated").value(false))
                .andExpect(jsonPath("$.meetingCount").value(count))
                .andExpect(jsonPath("$.summary").isNotEmpty());
    }

    @Test
    void dayRangeSummarizesThatDay() throws Exception {
        expectSummary(TUE, "day", 2);
    }

    @Test
    void weekRangeIncludesTheSameDay() throws Exception {
        expectSummary(TUE, "week", 2);
    }

    @Test
    void emptyRangeReportsNoMeetings() throws Exception {
        expectSummary(TUE.plusDays(1), "day", 0);
    }
}
