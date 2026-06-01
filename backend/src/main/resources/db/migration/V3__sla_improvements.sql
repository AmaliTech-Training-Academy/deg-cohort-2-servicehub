-- Add response-time tracking columns to service_requests
ALTER TABLE service_requests
    ADD COLUMN IF NOT EXISTS response_deadline TIMESTAMP,
    ADD COLUMN IF NOT EXISTS first_response_at TIMESTAMP;

-- Add performance indexes
CREATE INDEX IF NOT EXISTS idx_sr_status       ON service_requests (status);
CREATE INDEX IF NOT EXISTS idx_sr_category     ON service_requests (category);
CREATE INDEX IF NOT EXISTS idx_sr_requester    ON service_requests (requester_id);
CREATE INDEX IF NOT EXISTS idx_sr_assigned     ON service_requests (assigned_to_id);
CREATE INDEX IF NOT EXISTS idx_sr_sla_deadline ON service_requests (sla_deadline);
CREATE INDEX IF NOT EXISTS idx_sr_created_at   ON service_requests (created_at);

-- Migrate sla_policies to per-category + priority model
ALTER TABLE sla_policies ADD COLUMN IF NOT EXISTS category VARCHAR(50);

-- Expand from 4 priority-only rows to 12 category+priority rows
DELETE FROM sla_policies;

INSERT INTO sla_policies (id, category, priority, response_time_hours, resolution_time_hours) VALUES
  -- IT Support
  (1,  'IT_SUPPORT', 'CRITICAL', 1,  2),
  (2,  'IT_SUPPORT', 'HIGH',     2,  8),
  (3,  'IT_SUPPORT', 'MEDIUM',   4,  24),
  (4,  'IT_SUPPORT', 'LOW',      8,  48),
  -- HR
  (5,  'HR_REQUEST', 'CRITICAL', 1,  4),
  (6,  'HR_REQUEST', 'HIGH',     4,  24),
  (7,  'HR_REQUEST', 'MEDIUM',   8,  48),
  (8,  'HR_REQUEST', 'LOW',      24, 120),
  -- Facilities
  (9,  'FACILITIES', 'CRITICAL', 1,  4),
  (10, 'FACILITIES', 'HIGH',     4,  16),
  (11, 'FACILITIES', 'MEDIUM',   8,  48),
  (12, 'FACILITIES', 'LOW',      24, 72);

-- Replace priority-only unique constraint with composite (category, priority)
ALTER TABLE sla_policies DROP CONSTRAINT IF EXISTS sla_policies_priority_key;
ALTER TABLE sla_policies ADD CONSTRAINT uq_sla_category_priority UNIQUE (category, priority);

-- Reset sequence for new row count
SELECT setval('sla_policies_id_seq', (SELECT MAX(id) FROM sla_policies));
