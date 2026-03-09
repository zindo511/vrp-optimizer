CREATE TABLE locations (
    id            BIGSERIAL PRIMARY KEY,
    address       TEXT NOT NULL,
    latitude      DECIMAL(10, 7) NOT NULL,   -- Vi do
    longitude     DECIMAL(10, 7) NOT NULL,   -- Kinh do
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE depots (
    id            BIGSERIAL PRIMARY KEY,
    location_id   BIGINT NOT NULL UNIQUE REFERENCES locations(id),
    name          VARCHAR(255) NOT NULL,
    start_time    TIME,
    end_time      TIME
);

CREATE TABLE distance_matrix (
    id                BIGSERIAL PRIMARY KEY,
    origin_id         BIGINT NOT NULL REFERENCES locations(id),
    destination_id    BIGINT NOT NULL REFERENCES locations(id),
    distance_meters   DECIMAL(10,2) NOT NULL,
    duration_seconds  BIGINT NOT NULL,
    calculated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_od_pair UNIQUE(origin_id, destination_id)
);
CREATE INDEX idx_distance_matrix_lookup ON distance_matrix (origin_id, destination_id);
/*
Cơ sở hạ tầng về Địa lý.
Xây dựng bảng rập khuôn tọa độ (Location mapping).
Lưu metadata Distance Matrix phục vụ gọi và cache API Google Map.
 */