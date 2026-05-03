-- Seed default algorithm configs để frontend dropdown có data ngay từ đầu
-- Nếu không có seed này, fresh install → dropdown rỗng → không chạy được optimization

INSERT INTO algorithm_configs (name, population_size, generations,
    mutation_rate, crossover_rate, elitism_count, is_active, description)
VALUES
  ('NEAREST_NEIGHBOR', 100, 0, 0.05, 0.80, 2, true,
   'Thuật toán láng giềng gần nhất + 2-opt local search'),
  ('GENETIC_ALGORITHM', 100, 500, 0.05, 0.80, 2, false,
   'Thuật toán di truyền + 2-opt — kết quả tốt hơn, chạy chậm hơn');
