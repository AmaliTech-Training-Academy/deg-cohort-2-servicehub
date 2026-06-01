-- Add response deadline and first response tracking to support SLA breach detection
ALTER TABLE service_requests
    ADD COLUMN IF NOT EXISTS response_deadline  TIMESTAMP,
    ADD COLUMN IF NOT EXISTS first_response_at  TIMESTAMP;
