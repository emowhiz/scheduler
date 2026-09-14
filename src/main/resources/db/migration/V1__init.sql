CREATE TABLE users (
    id           UUID PRIMARY KEY,
    email        VARCHAR(255) NOT NULL UNIQUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE calendars (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    timezone   VARCHAR(64) NOT NULL DEFAULT 'UTC',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE meetings (
    id                 UUID PRIMARY KEY,
    organizer_user_id  UUID NOT NULL REFERENCES users (id),
    title              VARCHAR(255) NOT NULL,
    description        TEXT,
    start_at           TIMESTAMPTZ NOT NULL,
    end_at             TIMESTAMPTZ NOT NULL,
    status             VARCHAR(16) NOT NULL DEFAULT 'SCHEDULED',
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_meeting_time CHECK (end_at > start_at)
);
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE time_slots (
    id          UUID PRIMARY KEY,
    calendar_id UUID NOT NULL REFERENCES calendars (id) ON DELETE CASCADE,
    start_at    TIMESTAMPTZ NOT NULL,
    end_at      TIMESTAMPTZ NOT NULL,
    status      VARCHAR(16) NOT NULL DEFAULT 'FREE',
    meeting_id  UUID REFERENCES meetings (id) ON DELETE SET NULL,
    version     BIGINT NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_slot_time CHECK (end_at > start_at),
    CONSTRAINT excl_busy_overlap EXCLUDE USING gist (
        calendar_id WITH =,
        tstzrange(start_at, end_at) WITH &&
    ) WHERE (status = 'BUSY')
);
