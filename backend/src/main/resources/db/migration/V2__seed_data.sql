-- Departments: category values must match RequestCategory enum exactly
INSERT INTO departments (id, name, category, contact_email, is_active) VALUES
  (1, 'IT Support', 'IT_SUPPORT',  'it@amalitech.com',         true),
  (2, 'HR',         'HR_REQUEST',  'hr@amalitech.com',         true),
  (3, 'Facilities', 'FACILITIES',  'facilities@amalitech.com', true)
ON CONFLICT (id) DO NOTHING;

-- SLA policies
INSERT INTO sla_policies (id, priority, response_time_hours, resolution_time_hours) VALUES
  (1, 'LOW',      8, 48),
  (2, 'MEDIUM',   4, 24),
  (3, 'HIGH',     1,  4),
  (4, 'CRITICAL', 0,  2)
ON CONFLICT (id) DO NOTHING;

-- Default users (password: password123, bcrypt-hashed)
INSERT INTO users (id, email, full_name, password, role, created_at) VALUES
  (1, 'manager@amalitech.com', 'Manager User',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'MANAGER',  NOW()),
  (2, 'agent@amalitech.com',   'Support Agent', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AGENT',    NOW()),
  (3, 'user@amalitech.com',    'Test User',     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'EMPLOYEE', NOW())
ON CONFLICT (id) DO NOTHING;

-- Reset sequences so new inserts don't collide with seeded IDs
SELECT setval('users_id_seq',       (SELECT MAX(id) FROM users));
SELECT setval('departments_id_seq', (SELECT MAX(id) FROM departments));
SELECT setval('sla_policies_id_seq',(SELECT MAX(id) FROM sla_policies));
