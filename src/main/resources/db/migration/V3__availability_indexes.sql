
CREATE INDEX idx_time_slots_calendar_start ON time_slots (calendar_id, start_at);
CREATE INDEX idx_time_slots_calendar_free_start ON time_slots (calendar_id, start_at) WHERE status = 'FREE';