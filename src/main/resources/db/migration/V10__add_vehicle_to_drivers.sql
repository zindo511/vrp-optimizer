-- Gán mối quan hệ Driver ↔ Vehicle
-- Một driver lái một xe tại một thời điểm → UNIQUE constraint
-- ON DELETE SET NULL: xóa xe → driver vẫn còn, chỉ mất gán xe
ALTER TABLE drivers
    ADD COLUMN vehicle_id BIGINT UNIQUE
    REFERENCES vehicles(id) ON DELETE SET NULL;
