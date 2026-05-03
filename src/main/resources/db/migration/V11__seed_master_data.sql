-- 1. Seed Users (Mật khẩu mặc định: 123456)
INSERT INTO users (id, email, password, full_name, role, is_active)
VALUES
    (1, 'admin@vrp.com', '$2a$10$gNk9bC2LTsrKmd2nR22tcuyQsy87LdF9/K3.kN3JT6EZrMLE7cCci', 'System Admin', 'ADMIN', true),
    (2, 'dispatcher@vrp.com', '$2a$10$gNk9bC2LTsrKmd2nR22tcuyQsy87LdF9/K3.kN3JT6EZrMLE7cCci', 'Head Dispatcher', 'DISPATCHER', true),
    (3, 'driver1@vrp.com', '$2a$10$gNk9bC2LTsrKmd2nR22tcuyQsy87LdF9/K3.kN3JT6EZrMLE7cCci', 'Nguyen Van A', 'DRIVER', true),
    (4, 'driver2@vrp.com', '$2a$10$gNk9bC2LTsrKmd2nR22tcuyQsy87LdF9/K3.kN3JT6EZrMLE7cCci', 'Tran Van B', 'DRIVER', true)
ON CONFLICT (id) DO NOTHING;

SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));

-- 2. Seed Locations
INSERT INTO locations (id, address, latitude, longitude)
VALUES
    (1, 'Kho Tổng VRP Hà Nội, Cầu Giấy, HN', 21.028511, 105.804817)
ON CONFLICT (id) DO NOTHING;

SELECT setval('locations_id_seq', (SELECT MAX(id) FROM locations));

-- 3. Seed Depots
INSERT INTO depots (id, location_id, name, start_time, end_time)
VALUES
    (1, 1, 'Depot Cầu Giấy', '06:00:00', '22:00:00')
ON CONFLICT (id) DO NOTHING;

SELECT setval('depots_id_seq', (SELECT MAX(id) FROM depots));

-- 4. Seed Vehicle Types
INSERT INTO vehicle_types (id, name, max_weight_kg, max_volume_m3, cost_per_km, fixed_cost, average_speed_kmh)
VALUES
    (1, 'Xe Tải Nhẹ 1.5 Tấn', 1500, 8.0, 15000, 500000, 40.0),
    (2, 'Xe Tải Trung 5 Tấn', 5000, 25.0, 25000, 1000000, 35.0)
ON CONFLICT (id) DO NOTHING;

SELECT setval('vehicle_types_id_seq', (SELECT MAX(id) FROM vehicle_types));

-- 5. Seed Vehicles
INSERT INTO vehicles (id, vehicle_type_id, license_plate, status)
VALUES
    (1, 1, '29C-123.45', 'AVAILABLE'),
    (2, 2, '29C-678.90', 'AVAILABLE')
ON CONFLICT (id) DO NOTHING;

SELECT setval('vehicles_id_seq', (SELECT MAX(id) FROM vehicles));

-- 6. Seed Drivers
INSERT INTO drivers (id, user_id, license_number, phone, status, vehicle_id)
VALUES
    (1, 3, 'B2-001234', '0901234567', 'ACTIVE', 1),
    (2, 4, 'C-005678', '0912345678', 'ACTIVE', 2)
ON CONFLICT (id) DO NOTHING;

SELECT setval('drivers_id_seq', (SELECT MAX(id) FROM drivers));
