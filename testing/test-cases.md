# SyncSlot Test Cases

## TC001 - Create User
- Input: Valid user details
- Expected: User is created successfully
- Result: Pending

## TC002 - Create Availability
- Input: User availability with start and end time
- Expected: Availability is saved successfully
- Result: Pending

## TC003 - Create Appointment
- Input: Valid appointment details and participants
- Expected: Appointment is created successfully
- Result: Pending

## TC004 - Find Common Slot
- Input: Multiple participants with overlapping availability
- Expected: System suggests a common available slot
- Result: Pending

## TC005 - No Common Slot
- Input: Participants with no overlapping availability
- Expected: System suggests alternative slots
- Result: Pending

## TC006 - Buffer Conflict
- Input: Appointment with required buffer time
- Expected: System prevents a slot that violates the buffer
- Result: Pending

## TC007 - Availability Change
- Input: Change a user's availability after an appointment is created
- Expected: Affected appointment is detected
- Result: Pending

## TC008 - Automatic Rescheduling
- Input: Existing appointment becomes invalid
- Expected: System suggests suitable alternative slots
- Result: Pending

## TC009 - Priority Handling
- Input: Appointments with different priorities
- Expected: Higher-priority appointment is handled appropriately
- Result: Pending

## TC010 - Cascading Conflict
- Input: Rescheduling one appointment affects another appointment
- Expected: System detects the cascading conflict and suggests a feasible solution
- Result: Pending

## Main Demo Scenario

1. Alice is available from 09:00–12:00.
2. Bob is available from 10:00–13:00.
3. Charlie is available from 10:00–11:00.
4. Create a 60-minute project meeting.
5. Expected slot: 10:00–11:00.
6. Change Charlie's availability so the meeting is no longer valid.
7. System detects the affected appointment.
8. System searches for alternative slots.
9. If an alternative conflicts with another appointment, the system searches again.
10. Confirm the final suitable slot.
11. Verify that the appointment is updated.

### Expected Result
The system should detect conflicts and provide a practical alternative instead of simply rejecting the appointment.

## End-to-End Test

### Scenario
Three users need to attend a 60-minute meeting.

Alice:
09:00–12:00

Bob:
10:00–13:00

Charlie:
10:00–11:00

### Expected
The system should find:
10:00–11:00

### Change
Charlie becomes unavailable from 10:00–11:00.

### Expected After Change
1. Existing meeting is detected as affected.
2. System checks for conflicts.
3. System searches for another suitable slot.
4. System suggests an alternative.
5. After confirmation, the appointment is updated.
6. All participants see the updated appointment.