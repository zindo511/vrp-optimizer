-- Phase 6.2: Cost-aware optimization
-- Thêm column total_cost để lưu tổng chi phí vận hành (VND)
-- = Σ(fixedCost + costPerKm × distanceKm) cho tất cả tuyến
ALTER TABLE optimization_results
    ADD COLUMN total_cost DECIMAL(12, 2);
