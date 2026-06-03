-- Fix seeded user passwords — the V2 hash did not match "password123"
-- All three seed users now use password: password123
UPDATE users
SET password = '$2a$10$hUnvmiJfNEEM9dYe0C1/zeW3eJ9NkfD/.gtVeSkjysbzr.38r6VAi'
WHERE email IN ('manager@amalitech.com', 'agent@amalitech.com', 'user@amalitech.com');

-- Clean up temp users created during password verification
DELETE FROM users WHERE email IN ('testadmin@test.com', 'hashtest@test.com');
