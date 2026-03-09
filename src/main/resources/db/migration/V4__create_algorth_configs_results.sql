CREATE TABLE algorithm_configs (
    id                 BIGSERIAL PRIMARY KEY,
    name               VARCHAR(100) NOT NULL,
    population_size    INTEGER DEFAULT 100,
    generations        INTEGER DEFAULT 500,
    mutation_rate      DECIMAL(5,4) DEFAULT 0.05,
    crossover_rate     DECIMAL(5,4) DEFAULT 0.80,
    elitism_count      INTEGER DEFAULT 2,
    is_active          BOOLEAN DEFAULT FALSE,
    description        TEXT,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE optimization_results (
    id                 BIGSERIAL PRIMARY KEY,
    config_id          BIGINT NOT NULL REFERENCES algorithm_configs(id),
    run_by             BIGINT NOT NULL REFERENCES users(id),
    run_date           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_orders       INTEGER,
    total_vehicles     INTEGER,
    best_fitness       DECIMAL(12,4),
    total_distance     DECIMAL(10,2),
    execution_time_ms  BIGINT,
    status             VARCHAR(50),                 -- SUCCESS, FAILED, TIMEOUT
    error_message      TEXT
);
/*
Lưu trữ tất cả những parameter tinh chỉnh cần thiết của thuật toán GA (population_size, mutation_rate, generations,...)
Lưu History kết quả mỗi khi Admin nhấn nút Chạy (dành cho việc chấm điểm Fitness hoặc lấy báo cáo chạy).
 */