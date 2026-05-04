-- V10: Thêm cột capacity utilization cho optimization_results
-- Issue #11: Cho phép tracking hiệu quả sử dụng xe qua từng lần chạy
ALTER TABLE optimization_results ADD COLUMN IF NOT EXISTS avg_weight_utilization NUMERIC(5,2);
ALTER TABLE optimization_results ADD COLUMN IF NOT EXISTS avg_volume_utilization NUMERIC(5,2);
ALTER TABLE optimization_results ADD COLUMN IF NOT EXISTS unassigned_orders INTEGER;
