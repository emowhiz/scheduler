# Scheduler Service

## Description

Simple meeting scheduling platform simulation.

## Prerequisites

- Java 21
- Maven
- Docker
- PostgreSQL 18
- Spring Boot 4.1
- JUnit5
- Testcontainers

## How to Build and deploy the application

1. Clean and build the application:
    ```sh
    mvn clean install
    ```
2. Start the application and dependencies via docker compose
    ```sh
    docker compose up --build
    ```

This would setup the postgres and the application and flyway migrations would run on start.(test users and
calendars will also be created in the migration)

Then you can access api documentations and test out the apis using following urls

- Swagger docs: `http://localhost:8080/swagger-ui.html`
- OpenAPI spec: `http://localhost:8080/v3/api-docs`

## Design Decisions
Here lets talk about main design decisions to achieve the requirements given in [backend-challenge.pdf](../../../Desktop/backend-challenge/backend-challenge.pdf)

**Calendar is a domain-only concept.** 

The spec requires this explicitly. `Calendar` is an internal JPA entity in the `user.repository` package, should be 
created automatically alongside each `User` and never serialized into a request or response. The API speaks only in 
terms of users, slots, and meetings. Other packages reach it only through `CalendarService.getCalendarIdForUser(...)`.

**Double-booking is prevented by the database, not application code.** 

Table `time_slots` has a partial GiST exclusion constraint (needs the `btree_gist` extension):

```sql
EXCLUDE USING gist (calendar_id WITH =, tstzrange(start_at, end_at) WITH &&) WHERE (status = 'BUSY')
```
This makes sure two overlapping BUSY slots on the same calendar are physically impossible to insert. 
For two requests racing to update the *same* slot row `TimeSlot.version` (`@Version`) makes the loser's update fail as an
optimistic-lock conflict instead. Both failure modes map to a clean `409`, verified by
`MeetingConcurrencyTest`, which fires 10 concurrent booking requests at one slot and asserts
exactly one 201 and nine 409s.

**Booking mirrors busy time into every participant's calendar.** 

Converting a slot into a meeting marks the organizer's slot BUSY and inserts a new BUSY slot on each participant's
calendar in the same transaction, linked by `meeting_id`. Without this, a participant's availability view would
have no way to know they're in a meeting someone else organized. Trade-off: participants must be users in the system. 
Cancelling a meeting reverts the organizer's original slot to FREE (it existed before the booking) 
but *deletes* participants' mirrored slots(they only ever existed because of this booking).

## Consuming the service : walkthrough on the slot and meeting manipulation

Note: Test users are created here [V2__test_users.sql](src/main/resources/db/migration/V2__test_users.sql)
Execute following shell commands sequentially for the basic workflow walkthrough

```sh
BASE=http://localhost:8080/api/v1
````

1. alice@example.com declares a single free slot .

```sh
ALICE=11111111-1111-1111-1111-111111111111
SLOT=$(curl -s -X POST $BASE/slots -H 'Content-Type: application/json' \
-d '{"userId": "'"$ALICE"'" , "startAt":"2026-09-15T09:00:00Z","endAt":"2026-09-15T09:30:00Z"}' | jq -r .id)
```

2. alice@example.com books that slot as a meeting with bob@example.com as a participant

```sh
BOB=22222222-2222-2222-2222-222222222222
MEETING=$(curl -s -X POST $BASE/meetings/organizer/$ALICE/slot/$SLOT -H 'Content-Type: application/json' \
  -d '{"title":"Kickoff","description":"Project kickoff","participantUserIds":["'"$BOB"'"]}' | jq -r .id)
```
->  alice@example's slot becomes to BUSY, and a mirrored BUSY slot is created on bob@example.com's calendar too.

3. Cancel — Alice's slot returns to FREE, Bob's mirrored slot is removed
```sh
curl -s -X DELETE $BASE/meetings/$MEETING
```

## Known Issues and missing features

- **User flow** Currently there are no endpoints provided for user management flows. (can only be managed through 
  direct queries). With this initial domain specific calendar creation also happen as well. 
- **Auth.** There's none — endpoints trust the `userId` in the path. A real deployment needs
  authentication and to derive the caller's identity and authorize them instead of taking it as a URL parameter.
- **Availability Check** - Currently a check is performed in the backend when creating the meeting but there are no way
  to check for a particular users availability(aggregated view of free and busy slots)
- **Participant responses** — Currently system only checks for a existing busy slot and if not blocks a slot in the 
  participants calendar without any interaction from the participant. Introducing and managing participant responses 
  would be the next feature i would introduce.
- **Moving a meeting's time via PATCH** — currently unsupported to assign a different time slot.
- **Custom matrices** implementing custom matrices to capture domain specific changers. 