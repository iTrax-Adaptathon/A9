package com.syncslot.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    /** Meeting length in minutes (should equal endTime - startTime). */
    @Column(nullable = false)
    private int duration;

    /** Minutes padded before the meeting for conflict-checking purposes. */
    @Column(nullable = false)
    private int bufferBefore;

    /** Minutes padded after the meeting for conflict-checking purposes. */
    @Column(nullable = false)
    private int bufferAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "appointment_participants", joinColumns = @JoinColumn(name = "appointment_id"))
    @Column(name = "participant_id")
    private List<Long> participantIds = new ArrayList<>();

    public Appointment() {
    }

    public Appointment(String title, LocalDate date, LocalTime startTime, LocalTime endTime,
                       int duration, int bufferBefore, int bufferAfter,
                       Priority priority, AppointmentStatus status, List<Long> participantIds) {
        this.title = title;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.duration = duration;
        this.bufferBefore = bufferBefore;
        this.bufferAfter = bufferAfter;
        this.priority = priority;
        this.status = status;
        this.participantIds = participantIds != null ? participantIds : new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getBufferBefore() {
        return bufferBefore;
    }

    public void setBufferBefore(int bufferBefore) {
        this.bufferBefore = bufferBefore;
    }

    public int getBufferAfter() {
        return bufferAfter;
    }

    public void setBufferAfter(int bufferAfter) {
        this.bufferAfter = bufferAfter;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public List<Long> getParticipantIds() {
        return participantIds;
    }

    public void setParticipantIds(List<Long> participantIds) {
        this.participantIds = participantIds != null ? participantIds : new ArrayList<>();
    }
}
