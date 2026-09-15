package com.syncslot.config;

import com.syncslot.model.*;
import com.syncslot.repository.AppointmentRepository;
import com.syncslot.repository.AvailabilityRepository;
import com.syncslot.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Seeds the spec section 18 demo scenario on first startup (only when the
 * users table is empty): Alice, Bob, Charlie with availability plus the
 * "Team Catchup" appointment. Disable with app.seed-demo=false.
 */
@Component
@ConditionalOnProperty(name = "app.seed-demo", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AvailabilityRepository availabilityRepository;
    private final AppointmentRepository appointmentRepository;

    public DataSeeder(UserRepository userRepository,
                      AvailabilityRepository availabilityRepository,
                      AppointmentRepository appointmentRepository) {
        this.userRepository = userRepository;
        this.availabilityRepository = availabilityRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        LocalDate today = LocalDate.now();

        User alice = userRepository.save(new User("Alice", "alice@syncslot.local"));
        User bob = userRepository.save(new User("Bob", "bob@syncslot.local"));
        User charlie = userRepository.save(new User("Charlie", "charlie@syncslot.local"));

        availabilityRepository.save(new Availability(alice.getId(), today,
                LocalTime.of(9, 0), LocalTime.of(14, 0)));
        availabilityRepository.save(new Availability(bob.getId(), today,
                LocalTime.of(10, 0), LocalTime.of(15, 0)));
        availabilityRepository.save(new Availability(charlie.getId(), today,
                LocalTime.of(10, 0), LocalTime.of(12, 0)));

        appointmentRepository.save(new Appointment(
                "Team Catchup", today, LocalTime.of(10, 0), LocalTime.of(11, 0),
                60, 0, 0, Priority.LOW, AppointmentStatus.CONFIRMED,
                List.of(alice.getId(), bob.getId())));
    }
}
