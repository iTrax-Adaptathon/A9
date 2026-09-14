# SyncSlot API Tests

## User API
- POST /api/users — Create a user
- GET /api/users/{id} — Get user details
- PUT /api/users/{id} — Update user
- DELETE /api/users/{id} — Delete user

## Availability API
- POST /api/availability — Create availability
- GET /api/availability/{userId} — Get user availability
- PUT /api/availability/{id} — Update availability
- DELETE /api/availability/{id} — Delete availability

## Appointment API
- POST /api/appointments — Create appointment
- GET /api/appointments/{id} — Get appointment
- PUT /api/appointments/{id} — Update appointment
- DELETE /api/appointments/{id} — Delete appointment

## Scheduling API
- POST /api/schedule/find-slots — Find available slots
- POST /api/schedule/check-conflict — Check scheduling conflicts
- POST /api/schedule/reschedule — Reschedule an appointment
- GET /api/appointments/{id}/suggestions — Get alternative slots

## Testing Checklist
- [ ] Valid request
- [ ] Missing required fields
- [ ] Invalid data
- [ ] Conflict detection
- [ ] Alternative slot suggestions
- [ ] Rescheduling
- [ ] Error response

## Sample Test Data

### User 1
Name: Alice
Email: alice@example.com

### User 2
Name: Bob
Email: bob@example.com

### User 3
Name: Charlie
Email: charlie@example.com

### Meeting
Title: Project Meeting
Duration: 60 minutes
Participants: Alice, Bob, Charlie
Priority: High