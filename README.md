# SyncSlot: Smart Appointment Coordinator

SyncSlot is an appointment coordination system designed to move beyond simple scheduling by understanding conflicts, explaining its decisions, and enabling safe schedule changes. The system combines a React frontend, a Spring Boot backend, a pure Java scheduling engine, persistent storage, and an Electron desktop shell.

![SyncSlot Before vs After](docs/screenshots/comparison.png)

---

## Overview

| Before | Now |
| --- | --- |
| Basic appointment display | Multi-participant calendar workspace |
| Manual time selection | **Find Slots** with ranked recommendations |
| No conflict handling | Collision and conflict detection |
| No rescheduling safeguards | Preview → review → confirm cascade workflow |
| No schedule intelligence | AI-generated summaries and explanations |

---

## Features

**Calendar & Scheduling**
- Day, week, and month views
- Appointment creation with duration, buffers, and priority levels (Low, Medium, High, Urgent)
- Multi-participant appointments with availability-aware slot discovery
- Ranked alternative time slots, with filtering and list/calendar display modes

**Participant & Availability Management**
- Independent per-participant availability modeling
- Minute-level scheduling engine accounting for availability windows, existing appointments, buffers, priority, and disruption cost

---

## Intelligent Scheduling

Rather than simply checking whether a time is free, SyncSlot evaluates whether a proposed time is workable across all participants and surfaces the best alternatives.

Candidate slots are ranked using a configurable scoring model:

```
score = wAvail * availabilityFit
      + wPriority * priorityFit
      + wPref * preferenceFit
      + wBuffer * bufferQuality
      - wDisrupt * disruptionCost
```

Weights are configurable in `backend/.../algorithm/config/EngineConfig.java`.

---

## Conflict Detection & Safe Rescheduling

SyncSlot checks proposed appointments against existing schedules before making changes, via:

```
POST /api/schedule/find-slots
POST /api/schedule/check-conflict
```

When a conflict arises, SyncSlot does not move existing appointments automatically. Instead, it follows a controlled cascade:

```
Proposed appointment → Conflict check → Find alternatives → Preview cascade
→ User review → Explicit confirmation → Apply transactionally
```

The preview operation is read-only; changes are persisted only after confirmation, via:

```
POST /api/schedule/preview-cascade
POST /api/schedule/apply-cascade
```

![Schedule Change Review](docs/screenshots/schedule-review.png)

Each proposed change is reviewed with the affected appointments, participants, and conflicts, along with a plain-language explanation and explicit **Cancel** / **Confirm & Apply** actions.

---

## AI Schedule Summary

An AI layer summarizes the current schedule — meeting counts, times, priorities, participants, and notable items requiring attention.

```
POST /api/ai/summary
POST /api/ai/explain
```

A deterministic fallback summary is used when an LLM is unavailable, ensuring core scheduling functionality is not AI-dependent.

![AI Summary](docs/screenshots/ai-summary.png)

---

## Creating an Appointment

Appointments are created with title, date, start time, duration, buffers, priority, and participants. The **Find Slots** workflow then searches for feasible alternatives based on these constraints.

![New Appointment](docs/screenshots/new-appointment.png)

---

## Architecture

```
Electron Desktop Shell
        │
        ▼
React Frontend (Vite + JSX)
        │  HTTP / JSON
        ▼
Spring Boot API
  SchedulingService · ConflictService · CascadeService
  SlotRankingService · AiSummaryService · AiExplainService
        │
        ▼
Pure Java Scheduling Engine
  (minute-level scheduling, conflict detection, ranking, cascade planning)
        │
        ▼
JPA / Hibernate (H2 / optional MySQL)
```

All scheduling logic resides in the Java backend; the React frontend renders state returned by the REST API.

---

## Installation & Setup

**Prerequisites:** Java 17+, Maven 3.8+, Node.js 18+, npm

```bash
git clone https://github.com/notexactlynikhil/SynCal.git
cd SynCal
```

**Backend** (runs on `http://localhost:8080`, H2 by default):
```bash
cd backend
mvn spring-boot:run
```
For MySQL: `mvn spring-boot:run -Dspring-boot.run.profiles=mysql`

**Frontend** (runs on `http://localhost:5173`):
```bash
cd frontend
npm install
npm run dev
```

**Electron desktop shell:**
```bash
cd electron
npm install
npm start
```

Start services in order: backend → frontend → electron.

---

## REST API

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET/POST | `/api/users` | List / create participants |
| GET/POST/PUT/DELETE | `/api/availability` | Manage availability |
| GET/POST/PUT/DELETE | `/api/appointments` | Manage appointments |
| POST | `/api/schedule/find-slots` | Find and rank free slots |
| POST | `/api/schedule/check-conflict` | Detect conflicts and alternatives |
| POST | `/api/schedule/preview-cascade` | Preview a cascade without modifying data |
| POST | `/api/schedule/apply-cascade` | Apply a confirmed cascade |
| POST | `/api/ai/summary` | Generate a schedule summary |
| POST | `/api/ai/explain` | Generate a schedule explanation |

---

## Testing

```bash
cd backend
mvn test
```

Coverage includes the scheduling engine, conflict detection, slot ranking, cascade preview and persistence, API flows, and AI summary behavior (including deterministic fallback).

---

## Project Structure

```
SynCal/
├── backend/        # Spring Boot API + Java scheduling engine
├── frontend/        # React + Vite UI
├── electron/         # Desktop shell
└── docs/screenshots/
```

---

## Scheduling Safety Principles

1. Existing appointments are never moved silently.
2. Disruptive changes are previewed before being applied.
3. Cascades require explicit confirmation before persisting.

---

## Tech Stack

**Frontend:** React 18, JSX, Vite, HTML5/CSS3
---
**Backend:** Java 17, Spring Boot 3.3, Spring Data JPA, Hibernate, Maven
---
**Database:** H2 (default), MySQL (optional)
---
**Desktop:** Electron
---
**Testing:** JUnit, Spring Boot Test, MockMvc
---
**Intelligence:** Rule-based scheduling, conflict detection, ranked slot selection, bounded cascades, AI-assisted summaries with deterministic fallback

---

## License

This project currently does not specify a repository license.
