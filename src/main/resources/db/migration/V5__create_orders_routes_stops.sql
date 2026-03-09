CREATE TABLE orders (
    id               BIGSERIAL PRIMARY KEY,
    customer_name    VARCHAR(255) NOT NULL,
    customer_phone   VARCHAR(20),
    location_id      BIGINT NOT NULL REFERENCES locations(id),
    total_weight_kg  DECIMAL(10,2) NOT NULL,
    total_volume_m3  DECIMAL(10,2),
    note             TEXT,
    time_window_from TIME,                           -- Khung gio giao
    time_window_to   TIME,
    service_time_minutes INTEGER DEFAULT 15,         -- Thời gian phục vụ (bốc dỡ hàng, ký nhận) tại điểm giao
    status           VARCHAR(50) DEFAULT 'PENDING',  -- PENDING, ASSIGNED, IN_TRANSIT, COMPLETED, FAILED
    created_by       BIGINT REFERENCES users(id),    -- Người nhập đơn
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE routes (
    id                     BIGSERIAL PRIMARY KEY,
    optimization_result_id BIGINT REFERENCES optimization_results(id), -- Null nếu tạo thủ công
    start_depot_id         BIGINT NOT NULL REFERENCES depots(id),
    vehicle_id             BIGINT NOT NULL REFERENCES vehicles(id),
    driver_id              BIGINT REFERENCES drivers(id),              -- Null khi phân xe nhưng chưa chỉ định tài xế
    total_distance_meters  DECIMAL(10,2),
    total_duration_seconds BIGINT,
    total_weight_kg        DECIMAL(10,2),
    route_date             DATE NOT NULL,
    status                 VARCHAR(50) DEFAULT 'PLANNED',              -- PLANNED, IN_PROGRESS, COMPLETED
    created_at             TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE route_stops (
    id                     BIGSERIAL PRIMARY KEY,
    route_id               BIGINT NOT NULL REFERENCES routes(id),
    order_id               BIGINT REFERENCES orders(id),   -- Null nếu điểm dừng là về lại Kho (Depot)
    location_id            BIGINT NOT NULL REFERENCES locations(id),
    stop_order             INTEGER NOT NULL,               -- Thứ tự giao (1, 2, 3...)
    estimated_arrival      TIMESTAMP,
    actual_arrival         TIMESTAMP,                      -- Giờ tới thực tế
    status                 VARCHAR(50) DEFAULT 'WAITING',  -- WAITING, ARRIVED, COMPLETED, FAILED
    CONSTRAINT unique_stop_per_route UNIQUE (route_id, stop_order)
);

/*
Đây là phần cốt lõi của Database - Chứa Đơn hàng của khách hàng gửi tới orders.
Lưu trữ các Tuyến Đường (routes) do AI điều phối sinh ra.
Chi tiết những điểm Dừng Chân (route_stops) của một xe trên hành trình.
 */