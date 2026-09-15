WITH new_users AS (
  INSERT INTO users (id, email, created_at)
  VALUES
    ('11111111-1111-1111-1111-111111111111', 'alice@example.com', now()),
    ('22222222-2222-2222-2222-222222222222', 'bob@example.com',   now()),
    ('33333333-3333-3333-3333-333333333333', 'carol@example.com', now()),
    ('44444444-4444-4444-4444-444444444444', 'dave@example.com', now()),
    ('55555555-5555-5555-5555-555555555555', 'erin@example.com',  now())
  RETURNING id
)
INSERT INTO calendars (id, user_id, timezone, created_at)
SELECT gen_random_uuid(), id, 'UTC', now()
FROM new_users;