-- Thêm cột Driver vào Route
ALTER TABLE routes ADD COLUMN driver_id BIGINT;
ALTER TABLE routes ADD CONSTRAINT fk_routes_driver FOREIGN KEY (driver_id) REFERENCES drivers(id);

-- Thêm POD (Proof of Delivery) vào RouteStop
ALTER TABLE route_stops ADD COLUMN proof_image_url VARCHAR(255);
ALTER TABLE route_stops ADD COLUMN note VARCHAR(255);
ALTER TABLE route_stops ADD COLUMN failure_reason VARCHAR(255);

-- Thêm GPS tracking vào Driver
ALTER TABLE drivers ADD COLUMN current_lat DOUBLE PRECISION;
ALTER TABLE drivers ADD COLUMN current_lng DOUBLE PRECISION;