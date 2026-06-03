-- V5: Sample service request data for ETL testing, SLA breach testing, and dashboard development
-- 55 requests spread across all categories, priorities, and statuses
-- Includes: 7 overdue (past sla_deadline, not resolved), 22 resolved within SLA,
--           10 resolved with SLA breach, 16 active (future sla_deadline)
--
-- SLA policy reference (from V3 sla_policies table):
--   IT_SUPPORT: CRITICAL(res=2h,resp=1h) HIGH(8h,2h) MEDIUM(24h,4h) LOW(48h,8h)
--   HR_REQUEST: CRITICAL(4h,1h)          HIGH(24h,4h) MEDIUM(48h,8h) LOW(120h,24h)
--   FACILITIES: CRITICAL(4h,1h)          HIGH(16h,4h) MEDIUM(48h,8h) LOW(72h,24h)
--
-- department_id: IT_SUPPORT=1, HR_REQUEST=2, FACILITIES=3
-- Users: id=1 manager, id=2 agent (assigned_to), id=3 employee

INSERT INTO service_requests (
    id, title, description, category, priority, status,
    department_id, assigned_to_id, requester_id,
    sla_deadline, response_deadline, first_response_at,
    created_at, updated_at, resolved_at
) VALUES

-- =============================================================
-- SECTION A: OVERDUE (7 rows)
-- sla_deadline < NOW(), status NOT IN ('RESOLVED', 'CLOSED')
-- =============================================================

-- A1: Critical outage, OPEN 10 days, sla was 2h → overdue by ~10 days
(1, 'Production server unresponsive', 'All backend services down. No users can log in.',
    'IT_SUPPORT', 'CRITICAL', 'OPEN',
    1, NULL, 3,
    NOW() - INTERVAL '10 days' + INTERVAL '2 hours',
    NOW() - INTERVAL '10 days' + INTERVAL '1 hour',
    NULL,
    NOW() - INTERVAL '10 days',
    NOW() - INTERVAL '10 days',
    NULL),

-- A2: High priority IT, IN_PROGRESS 8 days, sla was 8h → overdue, agent responded but missed SLA
(2, 'VPN access broken for remote employees', 'Remote staff cannot connect to internal network.',
    'IT_SUPPORT', 'HIGH', 'IN_PROGRESS',
    1, 2, 3,
    NOW() - INTERVAL '8 days' + INTERVAL '8 hours',
    NOW() - INTERVAL '8 days' + INTERVAL '2 hours',
    NOW() - INTERVAL '8 days' + INTERVAL '3 hours',
    NOW() - INTERVAL '8 days',
    NOW() - INTERVAL '8 days' + INTERVAL '3 hours',
    NULL),

-- A3: High priority HR, OPEN 7 days, sla was 24h → overdue by ~6 days
(3, 'Payroll processing error for June', 'Salary calculations incorrect for 12 employees.',
    'HR_REQUEST', 'HIGH', 'OPEN',
    2, NULL, 1,
    NOW() - INTERVAL '7 days' + INTERVAL '24 hours',
    NOW() - INTERVAL '7 days' + INTERVAL '4 hours',
    NULL,
    NOW() - INTERVAL '7 days',
    NOW() - INTERVAL '7 days',
    NULL),

-- A4: Medium Facilities, ASSIGNED 6 days, sla was 48h → overdue by ~4 days
(4, 'Air conditioning failed in server room', 'Temperature rising. Risk of hardware damage.',
    'FACILITIES', 'MEDIUM', 'ASSIGNED',
    3, 2, 3,
    NOW() - INTERVAL '6 days' + INTERVAL '48 hours',
    NOW() - INTERVAL '6 days' + INTERVAL '8 hours',
    NOW() - INTERVAL '6 days' + INTERVAL '10 hours',
    NOW() - INTERVAL '6 days',
    NOW() - INTERVAL '6 days' + INTERVAL '10 hours',
    NULL),

-- A5: Medium IT, IN_PROGRESS 5 days, sla was 24h → overdue by ~4 days
(5, 'Email delivery failures intermittent', 'Some outgoing emails not reaching recipients.',
    'IT_SUPPORT', 'MEDIUM', 'IN_PROGRESS',
    1, 2, 2,
    NOW() - INTERVAL '5 days' + INTERVAL '24 hours',
    NOW() - INTERVAL '5 days' + INTERVAL '4 hours',
    NOW() - INTERVAL '5 days' + INTERVAL '2 hours',
    NOW() - INTERVAL '5 days',
    NOW() - INTERVAL '5 days' + INTERVAL '2 hours',
    NULL),

-- A6: Critical HR, OPEN 3 days, sla was 4h → overdue by ~3 days
(6, 'New employee onboarding blocked — no system access', 'Hire starts today. No accounts created yet.',
    'HR_REQUEST', 'CRITICAL', 'OPEN',
    2, NULL, 1,
    NOW() - INTERVAL '3 days' + INTERVAL '4 hours',
    NOW() - INTERVAL '3 days' + INTERVAL '1 hour',
    NULL,
    NOW() - INTERVAL '3 days',
    NOW() - INTERVAL '3 days',
    NULL),

-- A7: High Facilities, IN_PROGRESS 4 days, sla was 16h → overdue by ~3+ days
(7, 'Elevator out of service — floor 3 inaccessible', 'Mobility-impaired staff cannot reach their desks.',
    'FACILITIES', 'HIGH', 'IN_PROGRESS',
    3, 2, 3,
    NOW() - INTERVAL '4 days' + INTERVAL '16 hours',
    NOW() - INTERVAL '4 days' + INTERVAL '4 hours',
    NOW() - INTERVAL '4 days' + INTERVAL '5 hours',
    NOW() - INTERVAL '4 days',
    NOW() - INTERVAL '4 days' + INTERVAL '5 hours',
    NULL),

-- =============================================================
-- SECTION B: RESOLVED WITHIN SLA (12 rows)
-- resolved_at < sla_deadline, status RESOLVED or CLOSED
-- =============================================================

-- B1: IT CRITICAL, resolved in 1.5h < 2h SLA
(8, 'Database connection pool exhausted', 'Application throwing connection timeout errors.',
    'IT_SUPPORT', 'CRITICAL', 'RESOLVED',
    1, 2, 3,
    NOW() - INTERVAL '20 days' + INTERVAL '2 hours',
    NOW() - INTERVAL '20 days' + INTERVAL '1 hour',
    NOW() - INTERVAL '20 days' + INTERVAL '30 minutes',
    NOW() - INTERVAL '20 days',
    NOW() - INTERVAL '19 days' - INTERVAL '22 hours' - INTERVAL '30 minutes',
    NOW() - INTERVAL '19 days' - INTERVAL '22 hours' - INTERVAL '30 minutes'),

-- B2: IT HIGH, resolved in 2h < 8h SLA
(9, 'Shared drive permissions reset incorrectly', 'Finance team cannot access budget folder.',
    'IT_SUPPORT', 'HIGH', 'RESOLVED',
    1, 2, 1,
    NOW() - INTERVAL '15 days' + INTERVAL '8 hours',
    NOW() - INTERVAL '15 days' + INTERVAL '2 hours',
    NOW() - INTERVAL '15 days' + INTERVAL '1 hour',
    NOW() - INTERVAL '15 days',
    NOW() - INTERVAL '15 days' + INTERVAL '2 hours',
    NOW() - INTERVAL '15 days' + INTERVAL '2 hours'),

-- B3: HR LOW, resolved in 4 days < 5 days (120h) SLA
(10, 'Annual leave policy clarification request', 'Employee asking about carry-over entitlements.',
    'HR_REQUEST', 'LOW', 'RESOLVED',
    2, 2, 3,
    NOW() - INTERVAL '14 days' + INTERVAL '120 hours',
    NOW() - INTERVAL '14 days' + INTERVAL '24 hours',
    NOW() - INTERVAL '14 days' + INTERVAL '6 hours',
    NOW() - INTERVAL '14 days',
    NOW() - INTERVAL '10 days',
    NOW() - INTERVAL '10 days'),

-- B4: Facilities MEDIUM, resolved in 1 day < 2 days (48h) SLA
(11, 'Broken desk chair replacement needed', 'Chair armrest collapsed. Ergonomic issue.',
    'FACILITIES', 'MEDIUM', 'RESOLVED',
    3, 2, 2,
    NOW() - INTERVAL '12 days' + INTERVAL '48 hours',
    NOW() - INTERVAL '12 days' + INTERVAL '8 hours',
    NOW() - INTERVAL '12 days' + INTERVAL '4 hours',
    NOW() - INTERVAL '12 days',
    NOW() - INTERVAL '11 days',
    NOW() - INTERVAL '11 days'),

-- B5: IT LOW, resolved in 1 day < 2 days (48h) SLA, CLOSED
(12, 'Monitor flickering on second display', 'Intermittent display issue on dev workstation.',
    'IT_SUPPORT', 'LOW', 'CLOSED',
    1, 2, 3,
    NOW() - INTERVAL '15 days' + INTERVAL '48 hours',
    NOW() - INTERVAL '15 days' + INTERVAL '8 hours',
    NOW() - INTERVAL '15 days' + INTERVAL '5 hours',
    NOW() - INTERVAL '15 days',
    NOW() - INTERVAL '14 days',
    NOW() - INTERVAL '14 days'),

-- B6: HR MEDIUM, resolved in 1 day < 2 days (48h) SLA
(13, 'Request to update emergency contact details', 'Employee personal record needs updating.',
    'HR_REQUEST', 'MEDIUM', 'RESOLVED',
    2, 2, 3,
    NOW() - INTERVAL '10 days' + INTERVAL '48 hours',
    NOW() - INTERVAL '10 days' + INTERVAL '8 hours',
    NOW() - INTERVAL '10 days' + INTERVAL '3 hours',
    NOW() - INTERVAL '10 days',
    NOW() - INTERVAL '9 days',
    NOW() - INTERVAL '9 days'),

-- B7: Facilities CRITICAL, resolved in 3h < 4h SLA
(14, 'Water leak in storage room threatening equipment', 'Visible dripping near server rack. Urgent.',
    'FACILITIES', 'CRITICAL', 'RESOLVED',
    3, 2, 1,
    NOW() - INTERVAL '9 days' + INTERVAL '4 hours',
    NOW() - INTERVAL '9 days' + INTERVAL '1 hour',
    NOW() - INTERVAL '9 days' + INTERVAL '45 minutes',
    NOW() - INTERVAL '9 days',
    NOW() - INTERVAL '9 days' + INTERVAL '3 hours',
    NOW() - INTERVAL '9 days' + INTERVAL '3 hours'),

-- B8: IT MEDIUM, resolved in 12h < 24h SLA
(15, 'Password reset not working for external SSO', 'User locked out of corporate SSO account.',
    'IT_SUPPORT', 'MEDIUM', 'RESOLVED',
    1, 2, 2,
    NOW() - INTERVAL '7 days' + INTERVAL '24 hours',
    NOW() - INTERVAL '7 days' + INTERVAL '4 hours',
    NOW() - INTERVAL '7 days' + INTERVAL '2 hours',
    NOW() - INTERVAL '7 days',
    NOW() - INTERVAL '7 days' + INTERVAL '12 hours',
    NOW() - INTERVAL '7 days' + INTERVAL '12 hours'),

-- B9: HR HIGH, resolved in 4h < 24h SLA, CLOSED
(16, 'Overtime approval required for project deadline', 'Manager needs HR approval for team OT hours.',
    'HR_REQUEST', 'HIGH', 'CLOSED',
    2, 2, 1,
    NOW() - INTERVAL '6 days' + INTERVAL '24 hours',
    NOW() - INTERVAL '6 days' + INTERVAL '4 hours',
    NOW() - INTERVAL '6 days' + INTERVAL '2 hours',
    NOW() - INTERVAL '6 days',
    NOW() - INTERVAL '6 days' + INTERVAL '4 hours',
    NOW() - INTERVAL '6 days' + INTERVAL '4 hours'),

-- B10: Facilities LOW, resolved in 1 day < 3 days (72h) SLA
(17, 'Office plants need replacement — dead plants removal', 'Reception area plants wilted. Replacement requested.',
    'FACILITIES', 'LOW', 'RESOLVED',
    3, 2, 3,
    NOW() - INTERVAL '5 days' + INTERVAL '72 hours',
    NOW() - INTERVAL '5 days' + INTERVAL '24 hours',
    NOW() - INTERVAL '5 days' + INTERVAL '12 hours',
    NOW() - INTERVAL '5 days',
    NOW() - INTERVAL '4 days',
    NOW() - INTERVAL '4 days'),

-- B11: IT HIGH, resolved in 4h < 8h SLA
(18, 'CI/CD pipeline failing on main branch', 'Deployment blocked. Build server misconfigured.',
    'IT_SUPPORT', 'HIGH', 'RESOLVED',
    1, 2, 2,
    NOW() - INTERVAL '4 days' + INTERVAL '8 hours',
    NOW() - INTERVAL '4 days' + INTERVAL '2 hours',
    NOW() - INTERVAL '4 days' + INTERVAL '1 hour',
    NOW() - INTERVAL '4 days',
    NOW() - INTERVAL '4 days' + INTERVAL '4 hours',
    NOW() - INTERVAL '4 days' + INTERVAL '4 hours'),

-- B12: HR LOW, resolved in 2 days < 5 days (120h) SLA, CLOSED
(19, 'Request for employee training budget approval', 'Staff requesting external course reimbursement.',
    'HR_REQUEST', 'LOW', 'CLOSED',
    2, 2, 3,
    NOW() - INTERVAL '3 days' + INTERVAL '120 hours',
    NOW() - INTERVAL '3 days' + INTERVAL '24 hours',
    NOW() - INTERVAL '3 days' + INTERVAL '8 hours',
    NOW() - INTERVAL '3 days',
    NOW() - INTERVAL '1 day',
    NOW() - INTERVAL '1 day'),

-- =============================================================
-- SECTION C: RESOLVED WITH SLA BREACH (10 rows)
-- resolved_at > sla_deadline — historical breach data for ETL
-- =============================================================

-- C1: IT CRITICAL, resolved in 4 days >> 2h SLA — major breach
(20, 'Authentication service crash — all users logged out', 'JWT service down. Complete auth failure.',
    'IT_SUPPORT', 'CRITICAL', 'RESOLVED',
    1, 2, 3,
    NOW() - INTERVAL '21 days' + INTERVAL '2 hours',
    NOW() - INTERVAL '21 days' + INTERVAL '1 hour',
    NOW() - INTERVAL '21 days' + INTERVAL '3 hours',
    NOW() - INTERVAL '21 days',
    NOW() - INTERVAL '17 days',
    NOW() - INTERVAL '17 days'),

-- C2: IT HIGH, resolved in 4 days >> 8h SLA — significant breach
(21, 'Backup jobs failing silently for 2 weeks', 'No backups created. Discovered during audit.',
    'IT_SUPPORT', 'HIGH', 'RESOLVED',
    1, 2, 1,
    NOW() - INTERVAL '12 days' + INTERVAL '8 hours',
    NOW() - INTERVAL '12 days' + INTERVAL '2 hours',
    NOW() - INTERVAL '12 days' + INTERVAL '4 hours',
    NOW() - INTERVAL '12 days',
    NOW() - INTERVAL '8 days',
    NOW() - INTERVAL '8 days'),

-- C3: HR MEDIUM, resolved in 4 days >> 2 days (48h) SLA
(22, 'Incorrect deduction on payslip — employee complaint', 'Healthcare deduction applied twice in May.',
    'HR_REQUEST', 'MEDIUM', 'RESOLVED',
    2, 2, 3,
    NOW() - INTERVAL '11 days' + INTERVAL '48 hours',
    NOW() - INTERVAL '11 days' + INTERVAL '8 hours',
    NOW() - INTERVAL '11 days' + INTERVAL '10 hours',
    NOW() - INTERVAL '11 days',
    NOW() - INTERVAL '7 days',
    NOW() - INTERVAL '7 days'),

-- C4: Facilities HIGH, resolved in 3 days >> 16h SLA, CLOSED
(23, 'Parking barrier stuck — blocking staff exit', 'Barrier arm will not lift. Staff trapped in car park.',
    'FACILITIES', 'HIGH', 'CLOSED',
    3, 2, 2,
    NOW() - INTERVAL '10 days' + INTERVAL '16 hours',
    NOW() - INTERVAL '10 days' + INTERVAL '4 hours',
    NOW() - INTERVAL '10 days' + INTERVAL '6 hours',
    NOW() - INTERVAL '10 days',
    NOW() - INTERVAL '7 days',
    NOW() - INTERVAL '7 days'),

-- C5: IT LOW, resolved in 4 days >> 2 days (48h) SLA
(24, 'Old software license renewal reminder — past due', 'Adobe license expired. Design team unable to work.',
    'IT_SUPPORT', 'LOW', 'RESOLVED',
    1, 2, 1,
    NOW() - INTERVAL '8 days' + INTERVAL '48 hours',
    NOW() - INTERVAL '8 days' + INTERVAL '8 hours',
    NOW() - INTERVAL '8 days' + INTERVAL '24 hours',
    NOW() - INTERVAL '8 days',
    NOW() - INTERVAL '4 days',
    NOW() - INTERVAL '4 days'),

-- C6: HR CRITICAL, resolved in 2 days >> 4h SLA — response breach too
(25, 'Termination paperwork not processed — ex-employee access active', 'Former staff still has system access post-termination.',
    'HR_REQUEST', 'CRITICAL', 'RESOLVED',
    2, 2, 1,
    NOW() - INTERVAL '7 days' + INTERVAL '4 hours',
    NOW() - INTERVAL '7 days' + INTERVAL '1 hour',
    NOW() - INTERVAL '7 days' + INTERVAL '5 hours',
    NOW() - INTERVAL '7 days',
    NOW() - INTERVAL '5 days',
    NOW() - INTERVAL '5 days'),

-- C7: Facilities MEDIUM, resolved in 4 days >> 2 days (48h) SLA
(26, 'Projector in boardroom not displaying correctly', 'Display distorted during board meeting. Urgent fix needed.',
    'FACILITIES', 'MEDIUM', 'RESOLVED',
    3, 2, 3,
    NOW() - INTERVAL '6 days' + INTERVAL '48 hours',
    NOW() - INTERVAL '6 days' + INTERVAL '8 hours',
    NOW() - INTERVAL '6 days' + INTERVAL '9 hours',
    NOW() - INTERVAL '6 days',
    NOW() - INTERVAL '2 days',
    NOW() - INTERVAL '2 days'),

-- C8: IT MEDIUM, resolved in 4 days >> 1 day (24h) SLA, CLOSED
(27, 'Slack integration with ticketing system broken', 'Notifications not reaching team channel.',
    'IT_SUPPORT', 'MEDIUM', 'CLOSED',
    1, 2, 2,
    NOW() - INTERVAL '5 days' + INTERVAL '24 hours',
    NOW() - INTERVAL '5 days' + INTERVAL '4 hours',
    NOW() - INTERVAL '5 days' + INTERVAL '6 hours',
    NOW() - INTERVAL '5 days',
    NOW() - INTERVAL '1 day',
    NOW() - INTERVAL '1 day'),

-- C9: HR HIGH, resolved in 3 days >> 1 day (24h) SLA
(28, 'Benefits enrollment window missed — system error', 'Employee unable to select health plan due to portal bug.',
    'HR_REQUEST', 'HIGH', 'RESOLVED',
    2, 2, 3,
    NOW() - INTERVAL '4 days' + INTERVAL '24 hours',
    NOW() - INTERVAL '4 days' + INTERVAL '4 hours',
    NOW() - INTERVAL '4 days' + INTERVAL '5 hours',
    NOW() - INTERVAL '4 days',
    NOW() - INTERVAL '1 day',
    NOW() - INTERVAL '1 day'),

-- C10: Facilities LOW, resolved in 5 days >> 3 days (72h) SLA
(29, 'Roof gutter blocked — water pooling near entrance', 'Safety hazard. Slipping risk for staff.',
    'FACILITIES', 'LOW', 'RESOLVED',
    3, 2, 1,
    NOW() - INTERVAL '7 days' + INTERVAL '72 hours',
    NOW() - INTERVAL '7 days' + INTERVAL '24 hours',
    NOW() - INTERVAL '7 days' + INTERVAL '26 hours',
    NOW() - INTERVAL '7 days',
    NOW() - INTERVAL '2 days',
    NOW() - INTERVAL '2 days'),

-- =============================================================
-- SECTION D: ACTIVE — NOT OVERDUE (26 rows)
-- sla_deadline in the future; statuses OPEN, ASSIGNED, IN_PROGRESS
-- =============================================================

-- IT_SUPPORT active
(30, 'Laptop keyboard unresponsive after spill', 'Coffee spill on keyboard. Replacement needed.',
    'IT_SUPPORT', 'CRITICAL', 'OPEN',
    1, NULL, 3,
    NOW() + INTERVAL '1 hour',
    NOW() + INTERVAL '30 minutes',
    NULL,
    NOW() - INTERVAL '1 hour',
    NOW() - INTERVAL '1 hour',
    NULL),

(31, 'Wi-Fi drops in meeting room B', 'Connectivity unstable. Meetings disrupted.',
    'IT_SUPPORT', 'CRITICAL', 'ASSIGNED',
    1, 2, 1,
    NOW() + INTERVAL '1 hour' - INTERVAL '30 minutes',
    NOW() - INTERVAL '15 minutes',
    NOW() - INTERVAL '15 minutes',
    NOW() - INTERVAL '1 hour' - INTERVAL '30 minutes',
    NOW() - INTERVAL '15 minutes',
    NULL),

(32, 'GitHub access revoked for contractor', 'External dev lost repo access mid-sprint.',
    'IT_SUPPORT', 'HIGH', 'IN_PROGRESS',
    1, 2, 2,
    NOW() + INTERVAL '5 hours',
    NOW() + INTERVAL '1 hour',
    NOW() - INTERVAL '1 hour',
    NOW() - INTERVAL '3 hours',
    NOW() - INTERVAL '1 hour',
    NULL),

(33, 'Antivirus flagging false positive on dev tools', 'Build tool quarantined. Dev workflow blocked.',
    'IT_SUPPORT', 'HIGH', 'OPEN',
    1, NULL, 3,
    NOW() + INTERVAL '5 hours',
    NOW() + INTERVAL '1 hour',
    NULL,
    NOW() - INTERVAL '3 hours',
    NOW() - INTERVAL '3 hours',
    NULL),

(34, 'Shared printer on floor 2 offline', 'Printer not discoverable on network.',
    'IT_SUPPORT', 'MEDIUM', 'OPEN',
    1, NULL, 1,
    NOW() + INTERVAL '12 hours',
    NOW() + INTERVAL '16 hours',
    NULL,
    NOW() - INTERVAL '12 hours',
    NOW() - INTERVAL '12 hours',
    NULL),

(35, 'Slow internet speed in open-plan office', 'Bandwidth test shows 5Mbps vs expected 100Mbps.',
    'IT_SUPPORT', 'MEDIUM', 'IN_PROGRESS',
    1, 2, 3,
    NOW() + INTERVAL '6 hours',
    NOW() + INTERVAL '22 hours',
    NOW() - INTERVAL '16 hours',
    NOW() - INTERVAL '18 hours',
    NOW() - INTERVAL '16 hours',
    NULL),

(36, 'Software update required on design workstation', 'Figma desktop app outdated, missing features.',
    'IT_SUPPORT', 'LOW', 'OPEN',
    1, NULL, 2,
    NOW() + INTERVAL '36 hours',
    NOW() + INTERVAL '20 hours',
    NULL,
    NOW() - INTERVAL '12 hours',
    NOW() - INTERVAL '12 hours',
    NULL),

(37, 'Old laptop trade-in request', 'Device past end-of-life. Replacement procurement needed.',
    'IT_SUPPORT', 'LOW', 'ASSIGNED',
    1, 2, 1,
    NOW() + INTERVAL '12 hours',
    NOW() - INTERVAL '4 hours',
    NOW() - INTERVAL '4 hours',
    NOW() - INTERVAL '36 hours',
    NOW() - INTERVAL '4 hours',
    NULL),

-- HR_REQUEST active
(38, 'New hire equipment request — start date tomorrow', 'Laptop, badge, and system accounts needed.',
    'HR_REQUEST', 'CRITICAL', 'OPEN',
    2, NULL, 1,
    NOW() + INTERVAL '3 hours',
    NOW() + INTERVAL '30 minutes',
    NULL,
    NOW() - INTERVAL '1 hour',
    NOW() - INTERVAL '1 hour',
    NULL),

(39, 'Probation period review documentation missing', 'Line manager needs HR to provide review template.',
    'HR_REQUEST', 'CRITICAL', 'ASSIGNED',
    2, 2, 1,
    NOW() + INTERVAL '2 hours',
    NOW() - INTERVAL '1 hour' + INTERVAL '30 minutes',
    NOW() - INTERVAL '1 hour' + INTERVAL '30 minutes',
    NOW() - INTERVAL '2 hours',
    NOW() - INTERVAL '1 hour' + INTERVAL '30 minutes',
    NULL),

(40, 'Maternity leave paperwork submission', 'Employee requesting formal leave from next month.',
    'HR_REQUEST', 'HIGH', 'OPEN',
    2, NULL, 3,
    NOW() + INTERVAL '20 hours',
    NOW() + INTERVAL '2 hours',
    NULL,
    NOW() - INTERVAL '4 hours',
    NOW() - INTERVAL '4 hours',
    NULL),

(41, 'Salary review discussion request', 'Employee requesting annual compensation review meeting.',
    'HR_REQUEST', 'HIGH', 'IN_PROGRESS',
    2, 2, 2,
    NOW() + INTERVAL '16 hours',
    NOW() - INTERVAL '1 hour',
    NOW() - INTERVAL '1 hour',
    NOW() - INTERVAL '8 hours',
    NOW() - INTERVAL '1 hour',
    NULL),

(42, 'Request for reference letter', 'Employee requesting employment verification letter.',
    'HR_REQUEST', 'MEDIUM', 'OPEN',
    2, NULL, 3,
    NOW() + INTERVAL '24 hours',
    NOW() + INTERVAL '8 hours',
    NULL,
    NOW() - INTERVAL '24 hours',
    NOW() - INTERVAL '24 hours',
    NULL),

(43, 'Team offsite planning — venue booking approval', 'HR sign-off needed for team building budget.',
    'HR_REQUEST', 'MEDIUM', 'ASSIGNED',
    2, 2, 1,
    NOW() + INTERVAL '24 hours',
    NOW() - INTERVAL '16 hours',
    NOW() - INTERVAL '16 hours',
    NOW() - INTERVAL '2 days',
    NOW() - INTERVAL '16 hours',
    NULL),

(44, 'Update to job description — promotion request', 'Employee promoted. Role description needs updating.',
    'HR_REQUEST', 'LOW', 'OPEN',
    2, NULL, 2,
    NOW() + INTERVAL '3 days',
    NOW() + INTERVAL '2 hours',
    NULL,
    NOW() - INTERVAL '2 days',
    NOW() - INTERVAL '2 days',
    NULL),

(45, 'HR records audit — missing documentation', 'Annual audit requires employee file completion.',
    'HR_REQUEST', 'LOW', 'IN_PROGRESS',
    2, 2, 1,
    NOW() + INTERVAL '2 days',
    NOW() - INTERVAL '4 hours',
    NOW() - INTERVAL '4 hours',
    NOW() - INTERVAL '3 days',
    NOW() - INTERVAL '4 hours',
    NULL),

-- FACILITIES active
(46, 'Fire alarm panel showing fault warning', 'Panel fault light active. Safety compliance risk.',
    'FACILITIES', 'CRITICAL', 'OPEN',
    3, NULL, 3,
    NOW() + INTERVAL '3 hours',
    NOW() + INTERVAL '30 minutes',
    NULL,
    NOW() - INTERVAL '1 hour',
    NOW() - INTERVAL '1 hour',
    NULL),

(47, 'Emergency exit light not functioning', 'Exit sign on level 2 stairwell is dark.',
    'FACILITIES', 'CRITICAL', 'ASSIGNED',
    3, 2, 2,
    NOW() + INTERVAL '2 hours',
    NOW() - INTERVAL '30 minutes',
    NOW() - INTERVAL '30 minutes',
    NOW() - INTERVAL '2 hours',
    NOW() - INTERVAL '30 minutes',
    NULL),

(48, 'Broken window latch in office — security risk', 'Window on ground floor cannot be locked.',
    'FACILITIES', 'HIGH', 'OPEN',
    3, NULL, 1,
    NOW() + INTERVAL '12 hours',
    NOW() + INTERVAL '2 hours',
    NULL,
    NOW() - INTERVAL '4 hours',
    NOW() - INTERVAL '4 hours',
    NULL),

(49, 'Heating not working in north wing', 'Thermostats unresponsive. Staff working in cold.',
    'FACILITIES', 'HIGH', 'IN_PROGRESS',
    3, 2, 3,
    NOW() + INTERVAL '12 hours',
    NOW() + INTERVAL '2 hours',
    NOW() - INTERVAL '2 hours',
    NOW() - INTERVAL '4 hours',
    NOW() - INTERVAL '2 hours',
    NULL),

(50, 'Kitchen dishwasher leaking', 'Water pooling under unit. Slip hazard.',
    'FACILITIES', 'MEDIUM', 'OPEN',
    3, NULL, 2,
    NOW() + INTERVAL '36 hours',
    NOW() + INTERVAL '44 hours',
    NULL,
    NOW() - INTERVAL '12 hours',
    NOW() - INTERVAL '12 hours',
    NULL),

(51, 'Desk allocation for new team — floor plan update', 'New analytics team needs 6 workstations arranged.',
    'FACILITIES', 'MEDIUM', 'ASSIGNED',
    3, 2, 1,
    NOW() + INTERVAL '24 hours',
    NOW() - INTERVAL '8 hours',
    NOW() - INTERVAL '8 hours',
    NOW() - INTERVAL '1 day',
    NOW() - INTERVAL '8 hours',
    NULL),

(52, 'Request for additional storage shelving', 'Storage room overflow. Need 3 extra shelving units.',
    'FACILITIES', 'LOW', 'OPEN',
    3, NULL, 3,
    NOW() + INTERVAL '60 hours',
    NOW() + INTERVAL '12 hours',
    NULL,
    NOW() - INTERVAL '12 hours',
    NOW() - INTERVAL '12 hours',
    NULL),

(53, 'Car park line markings faded — repaint request', 'Parking bays unclear. Causing congestion daily.',
    'FACILITIES', 'LOW', 'IN_PROGRESS',
    3, 2, 2,
    NOW() + INTERVAL '48 hours',
    NOW() - INTERVAL '4 hours',
    NOW() - INTERVAL '4 hours',
    NOW() - INTERVAL '2 days',
    NOW() - INTERVAL '4 hours',
    NULL),

(54, 'Flickering fluorescent light in corridor', 'Lights on level 1 corridor strobing. Headache risk.',
    'IT_SUPPORT', 'HIGH', 'IN_PROGRESS',
    1, 2, 1,
    NOW() + INTERVAL '4 hours',
    NOW() + INTERVAL '2 hours',
    NOW() - INTERVAL '1 hour',
    NOW() - INTERVAL '4 hours',
    NOW() - INTERVAL '1 hour',
    NULL),

(55, 'Request for ergonomic keyboard for wrist injury', 'Employee has RSI. Doctor recommends ergonomic keyboard.',
    'HR_REQUEST', 'MEDIUM', 'OPEN',
    2, NULL, 3,
    NOW() + INTERVAL '42 hours',
    NOW() + INTERVAL '18 hours',
    NULL,
    NOW() - INTERVAL '6 hours',
    NOW() - INTERVAL '6 hours',
    NULL);

-- Reset sequence so new inserts don't collide with seeded IDs
SELECT setval('service_requests_id_seq', (SELECT MAX(id) FROM service_requests));
