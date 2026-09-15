package com.syncslot.service;

import com.syncslot.dto.AiSummaryRequest;
import com.syncslot.dto.AiSummaryResponse;
import com.syncslot.model.Appointment;
import com.syncslot.model.User;
import com.syncslot.repository.AppointmentRepository;
import com.syncslot.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Summarises the schedule for a day, week or month in plain language.
 *
 * <p>Like {@link AiExplainService}, the model is only a narrator: the meetings
 * come straight from the database and are never changed. When the LLM is
 * unavailable a deterministic summary built in Java is returned instead.
 */
@Service
public class AiSummaryService {

    private static final Logger log = LoggerFactory.getLogger(AiSummaryService.class);

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private static final String SYSTEM_PROMPT = """
            You are SyncSlot's schedule summarizer.
            Summarize the user's scheduled meetings in a short, friendly paragraph of 3-5 sentences.
            If the range spans more than one day, give an overview by day and call out the busiest day.
            Mention urgent/high priority meetings and how many people they involve.
            You must NOT make, change or suggest scheduling decisions - only summarize what is given.
            Do not invent meetings or details that are not present.
            """;

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final LlmClient llmClient;

    public AiSummaryService(AppointmentRepository appointmentRepository,
                            UserRepository userRepository,
                            LlmClient llmClient) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.llmClient = llmClient;
    }

    public AiSummaryResponse summarize(AiSummaryRequest request) {
        LocalDate date = (request == null || request.date() == null) ? LocalDate.now() : request.date();
        String range = (request == null || request.range() == null)
                ? "day" : request.range().toLowerCase(Locale.ROOT);

        LocalDate start;
        LocalDate end;
        switch (range) {
            case "week" -> {
                start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                end = start.plusDays(6);
            }
            case "month" -> {
                start = date.withDayOfMonth(1);
                end = date.withDayOfMonth(date.lengthOfMonth());
            }
            default -> {
                range = "day";
                start = date;
                end = date;
            }
        }

        String period = periodLabel(range, start, end);
        List<Appointment> meetings =
                appointmentRepository.findByDateBetweenOrderByDateAscStartTimeAsc(start, end);

        if (meetings.isEmpty()) {
            return new AiSummaryResponse("No meetings scheduled " + period + ".", false, 0, period);
        }

        Map<Long, String> names = userNames();
        String fallback = deterministicSummary(meetings, names, period);

        if (!llmClient.isConfigured()) {
            log.debug("app.ai.api-key is not set; returning the deterministic summary");
            return new AiSummaryResponse(fallback, false, meetings.size(), period);
        }
        try {
            String summary = llmClient.complete(SYSTEM_PROMPT, buildUserPrompt(meetings, names, period, start, end));
            if (summary == null || summary.isBlank()) {
                return new AiSummaryResponse(fallback, false, meetings.size(), period);
            }
            return new AiSummaryResponse(summary.trim(), true, meetings.size(), period);
        } catch (Exception ex) {
            log.warn("AI summary request failed ({}); returning the deterministic summary", ex.getMessage());
            return new AiSummaryResponse(fallback, false, meetings.size(), period);
        }
    }

    private String periodLabel(String range, LocalDate start, LocalDate end) {
        return switch (range) {
            case "week" -> "this week (" + start + " to " + end + ")";
            case "month" -> "this month (" + start + " to " + end + ")";
            default -> "on " + start;
        };
    }

    private String buildUserPrompt(List<Appointment> meetings, Map<Long, String> names,
                                   String period, LocalDate start, LocalDate end) {
        StringBuilder sb = new StringBuilder();
        sb.append("Period: ").append(period).append(" (").append(start).append(" to ").append(end).append(")\n");
        sb.append("Total meetings: ").append(meetings.size()).append("\n");
        sb.append("Meetings:\n");
        for (Appointment a : meetings) {
            sb.append("- ").append(a.getDate()).append(' ')
                    .append(a.getStartTime().format(TIME)).append('-').append(a.getEndTime().format(TIME))
                    .append(": ").append(a.getTitle())
                    .append(" (priority ").append(a.getPriority())
                    .append(", status ").append(a.getStatus())
                    .append(", participants: ").append(participantNames(a, names))
                    .append(")\n");
        }
        return sb.toString();
    }

    private String deterministicSummary(List<Appointment> meetings, Map<Long, String> names, String period) {
        StringBuilder sb = new StringBuilder();
        sb.append(meetings.size()).append(meetings.size() == 1 ? " meeting " : " meetings ").append(period).append(": ");
        for (int i = 0; i < meetings.size(); i++) {
            Appointment a = meetings.get(i);
            if (i > 0) sb.append(", ");
            sb.append(a.getTitle()).append(' ')
                    .append(a.getStartTime().format(TIME)).append('-').append(a.getEndTime().format(TIME));
            if (meetings.size() <= 5) {
                sb.append(" (").append(a.getPriority()).append(')');
            }
        }
        return sb.toString();
    }

    private String participantNames(Appointment a, Map<Long, String> names) {
        if (a.getParticipantIds() == null || a.getParticipantIds().isEmpty()) {
            return "none";
        }
        return a.getParticipantIds().stream()
                .map(id -> names.getOrDefault(id, "#" + id))
                .collect(Collectors.joining(", "));
    }

    private Map<Long, String> userNames() {
        return userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, User::getName));
    }
}
