CREATE TABLE vehicle_types (
    id                 BIGSERIAL PRIMARY KEY,
    name               VARCHAR(100) NOT NULL,
    max_weight_kg      DECIMAL(10,2) NOT NULL,
    max_volume_m3      DECIMAL(10,2),
    max_driving_time_minutes          INTEGER,
    cost_per_km        DECIMAL(10,2) NOT NULL,
    fixed_cost         DECIMAL(10,2) DEFAULT 0,
    average_speed_kmh  DECIMAL(5,2) DEFAULT 40.0,
    is_active          BOOLEAN DEFAULT TRUE
);

CREATE TABLE vehicles (
    id               BIGSERIAL PRIMARY KEY,
    vehicle_type_id  BIGINT NOT NULL REFERENCES vehicle_types(id),
    license_plate    VARCHAR(50) NOT NULL UNIQUE,
    status           VARCHAR(50) DEFAULT 'AVAILABLE', -- AVAILABLE, IN_TRANSIT, MAINTENANCE
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE drivers (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL UNIQUE REFERENCES users(id),
    license_number   VARCHAR(100) NOT NULL UNIQUE,
    phone            VARCHAR(20) NOT NULL,
    status           VARCHAR(50) DEFAULT 'ACTIVE'  -- ACTIVE, INACTIVE, ON_LEAVE
);
/*
Mọi ràng buộc vật lý phục vụ tính toán thuật toán nằm ở bảng vehicle_types (Tải trọng, rào cản chi phí theo KM).
Quản lý xe thật (vehicles) và tài xế ứng với xe đó (drivers).
 */