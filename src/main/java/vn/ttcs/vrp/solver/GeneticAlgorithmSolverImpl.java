package vn.ttcs.vrp.solver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.ttcs.vrp.dto.vrp.PlannedRouteDto;
import vn.ttcs.vrp.dto.vrp.VrpSolutionDto;
import vn.ttcs.vrp.model.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Genetic Algorithm Solver cho Vehicle Routing Problem (VRP)
 *
 * ═══════════════════════════════════════════════════════════════════
 * TẠI SAO DÙNG GENETIC ALGORITHM?
 * ═══════════════════════════════════════════════════════════════════
 *
 * Nearest Neighbor + 2-opt tìm LOCAL OPTIMUM tốt cho từng tuyến riêng lẻ,
 * nhưng không thể:
 *   - Chuyển đơn giữa các xe (inter-route optimization)
 *   - Thử nghiệm các cách phân đơn hoàn toàn khác nhau
 *   - Thoát khỏi local optimum để tìm GLOBAL OPTIMUM
 *
 * GA mô phỏng tiến hóa tự nhiên: duy trì một QUẦN THỂ (population)
 * các giải pháp, cho chúng LAI GHÉP (crossover) và ĐỘT BIẾN (mutation)
 * để khám phá không gian tìm kiếm rộng hơn.
 *
 * ═══════════════════════════════════════════════════════════════════
 * CHROMOSOME ENCODING
 * ═══════════════════════════════════════════════════════════════════
 *
 * Mỗi chromosome = 1 hoán vị (permutation) của tất cả đơn hàng.
 *
 * Ví dụ 6 đơn hàng, 2 xe:
 *   Chromosome: [3, 1, 5, 2, 6, 4]
 *
 * Decode: quét từ trái sang phải, gán lần lượt vào xe.
 *   Xe 1: đơn 3 → 1 → 5 (đến đơn 2 thì vượt tải trọng → chuyển xe)
 *   Xe 2: đơn 2 → 6 → 4
 *
 * Tại sao dùng encoding này?
 *   → Đơn giản, mọi hoán vị đều hợp lệ (không tạo chromosome invalid)
 *   → Crossover/mutation giữ nguyên tính hoán vị → không cần repair
 *   → Decode tự nhiên: thứ tự gần nhau → cùng xe → bảo tồn cụm tốt
 *
 * ═══════════════════════════════════════════════════════════════════
 * FITNESS FUNCTION
 * ═══════════════════════════════════════════════════════════════════
 *
 * fitness = 1.0 / totalCost
 * totalCost = Σ (fixedCost + costPerKm × distanceKm) cho mỗi xe sử dụng
 *           + unassignedPenalty (10,000,000 × số đơn chưa phân)
 *
 * Dùng COST thay vì DISTANCE để tích hợp cost-aware (6.2):
 *   - Xe rẻ chạy xa = cost thấp = fitness cao
 *   - Xe đắt chạy gần = cost vẫn có thể cao hơn
 *
 * Phạt đơn chưa phân rất nặng để GA luôn ưu tiên phân hết đơn.
 *
 * ═══════════════════════════════════════════════════════════════════
 * CÁC TOÁN TỬ DI TRUYỀN
 * ═══════════════════════════════════════════════════════════════════
 *
 * 1. SELECTION: Tournament Selection (size = 3)
 *    → Chọn ngẫu nhiên 3 cá thể, giữ cá thể tốt nhất
 *    → Tại sao không dùng Roulette Wheel? Tournament ổn định hơn,
 *      không bị thiên vị quá mạnh bởi cá thể siêu tốt,
 *      duy trì diversity tốt hơn.
 *
 * 2. CROSSOVER: Order Crossover (OX)
 *    → Chọn 1 đoạn từ parent1, điền phần còn lại theo thứ tự parent2
 *    → Tại sao OX? Giữ nguyên thứ tự tương đối của các đơn →
 *      bảo tồn "building blocks" (cụm đơn gần nhau trên cùng tuyến)
 *
 * 3. MUTATION: Swap Mutation
 *    → Đổi chỗ 2 đơn ngẫu nhiên
 *    → Tại sao swap? Đơn giản, không phá vỡ permutation, tạo
 *      diversity đủ mà không quá aggressive phá cấu trúc tốt
 *
 * 4. ELITISM: Giữ N cá thể tốt nhất sang thế hệ mới
 *    → Đảm bảo quality KHÔNG BAO GIỜ giảm qua các thế hệ
 */
@Component("geneticAlgorithmSolver")
@RequiredArgsConstructor
@Slf4j(topic = "GA-SOLVER")
public class GeneticAlgorithmSolverImpl implements VrpSolver {

    private static final double UNASSIGNED_PENALTY = 10_000_000.0;
    private static final int TOURNAMENT_SIZE = 3;

    private final TwoOptLocalSearch twoOptLocalSearch;
    private final Random random = new Random();

    @Override
    public VrpSolutionDto solve(
            List<Order> orders,
            List<Vehicle> vehicles,
            Location depotLocation,
            Map<String, DistanceMatrix> distanceMap,
            AlgorithmConfig config,
            LocalDate routeDate,
            LocalTime depotStartTime) {

        int populationSize = config.getPopulationSize() != null ? config.getPopulationSize() : 100;
        int generations    = config.getGenerations() != null ? config.getGenerations() : 500;
        double mutationRate  = config.getMutationRate() != null
                ? config.getMutationRate().doubleValue() : 0.05;
        double crossoverRate = config.getCrossoverRate() != null
                ? config.getCrossoverRate().doubleValue() : 0.80;
        int elitismCount    = config.getElitismCount() != null ? config.getElitismCount() : 2;

        log.info("GA bắt đầu: {} đơn, {} xe | pop={}, gen={}, mut={}, cross={}, elite={}",
                orders.size(), vehicles.size(),
                populationSize, generations, mutationRate, crossoverRate, elitismCount);

        // ═════════════════════════════════════════════════════════════════════
        // COST-AWARE (6.2): Sắp xếp xe theo costPerKm tăng dần
        // ═════════════════════════════════════════════════════════════════════
        // Khi decode chromosome, xe rẻ được gán trước → đơn nhẹ tự nhiên
        // rơi vào xe rẻ → giảm tổng chi phí.
        // ═════════════════════════════════════════════════════════════════════
        List<Vehicle> sortedVehicles = vehicles.stream()
                .sorted(Comparator.comparing(v -> v.getVehicleType().getCostPerKm()))
                .collect(Collectors.toList());

        // ── Bước 1: Khởi tạo quần thể ──────────────────────────────────────
        List<List<Order>> population = initializePopulation(
                orders, populationSize);

        double[] fitnessArray = new double[populationSize];
        int bestIdx = 0;
        double bestFitness = 0;

        // ── Bước 2: Vòng lặp tiến hóa ──────────────────────────────────────
        for (int gen = 0; gen < generations; gen++) {

            // Đánh giá fitness
            for (int i = 0; i < population.size(); i++) {
                fitnessArray[i] = evaluateFitness(
                        population.get(i), sortedVehicles, depotLocation,
                        distanceMap, routeDate, depotStartTime);
            }

            // Tìm cá thể tốt nhất
            for (int i = 0; i < population.size(); i++) {
                if (fitnessArray[i] > bestFitness) {
                    bestFitness = fitnessArray[i];
                    bestIdx = i;
                }
            }

            // Log tiến trình mỗi 10% hoặc thế hệ cuối
            if (gen % (generations / 10 + 1) == 0 || gen == generations - 1) {
                double bestCost = bestFitness > 0 ? 1.0 / bestFitness : Double.MAX_VALUE;
                log.info("Gen {}/{}: bestFitness={:.6f}, bestCost={:,.0f}đ"
                                .replace("{:.6f}", "%.6f")
                                .replace("{:,.0f}", "%,.0f"),
                        gen + 1, generations, bestFitness, bestCost);
            }

            // Tạo thế hệ mới
            List<List<Order>> newPopulation = new ArrayList<>(populationSize);

            // ── ELITISM: giữ N cá thể tốt nhất ─────────────────────────────
            // Tại sao cần elitism? Nếu không giữ elite, crossover + mutation
            // có thể vô tình phá hủy cá thể tốt nhất → quality dao động
            // thay vì tăng đều. Elitism đảm bảo monotonic improvement.
            // ─────────────────────────────────────────────────────────────────
            List<Integer> eliteIndices = getTopIndices(fitnessArray, elitismCount);
            for (int idx : eliteIndices) {
                newPopulation.add(new ArrayList<>(population.get(idx)));
            }

            // ── Sinh phần còn lại bằng selection + crossover + mutation ─────
            while (newPopulation.size() < populationSize) {
                List<Order> parent1 = tournamentSelection(population, fitnessArray);
                List<Order> parent2 = tournamentSelection(population, fitnessArray);

                List<Order> child;
                if (random.nextDouble() < crossoverRate) {
                    child = orderCrossover(parent1, parent2);
                } else {
                    child = new ArrayList<>(parent1);
                }

                if (random.nextDouble() < mutationRate) {
                    swapMutation(child);
                }

                newPopulation.add(child);
            }

            population = newPopulation;
        }

        // ── Bước 3: Đánh giá lần cuối và lấy cá thể tốt nhất ──────────────
        for (int i = 0; i < population.size(); i++) {
            fitnessArray[i] = evaluateFitness(
                    population.get(i), sortedVehicles, depotLocation,
                    distanceMap, routeDate, depotStartTime);
        }
        bestIdx = 0;
        bestFitness = fitnessArray[0];
        for (int i = 1; i < population.size(); i++) {
            if (fitnessArray[i] > bestFitness) {
                bestFitness = fitnessArray[i];
                bestIdx = i;
            }
        }

        // ── Bước 4: Decode cá thể tốt nhất thành solution ──────────────────
        List<Order> bestChromosome = population.get(bestIdx);
        VrpSolutionDto solution = decodeToSolution(
                bestChromosome, sortedVehicles, depotLocation,
                distanceMap, routeDate, depotStartTime);

        // ── Bước 5: Chạy 2-opt trên từng tuyến (6.1) ───────────────────────
        // GA tìm phân bổ đơn-xe tốt (inter-route), nhưng thứ tự stop trong
        // mỗi tuyến có thể chưa tối ưu. 2-opt cải thiện intra-route.
        // Kết hợp GA + 2-opt = tốt nhất cả 2 mặt.
        // ─────────────────────────────────────────────────────────────────────
        solution = applyTwoOptToAllRoutes(solution, depotLocation, distanceMap, routeDate, depotStartTime);

        double bestCost = bestFitness > 0 ? 1.0 / bestFitness : 0;
        log.info("GA hoàn thành! {} tuyến, {} đơn chưa phân. Tổng: {}km — {}đ",
                solution.getRoutes().size(),
                solution.getUnassignedOrderCount(),
                String.format("%.2f", solution.getTotalDistanceMeters() / 1000),
                String.format("%,.0f", solution.getTotalCostVnd()));

        return solution;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // KHỞI TẠO QUẦN THỂ
    // ═════════════════════════════════════════════════════════════════════════
    // Tạo populationSize hoán vị ngẫu nhiên. Cá thể đầu tiên giữ nguyên
    // thứ tự gốc (thường là thứ tự NN nếu orders đã được sắp xếp trước).
    //
    // Tại sao dùng random shuffle thay vì heuristic phức tạp hơn?
    //   → Random đảm bảo DIVERSITY cao → GA khám phá nhiều vùng khác nhau
    //   → Heuristic tạo ra các cá thể quá giống nhau → premature convergence
    // ═════════════════════════════════════════════════════════════════════════
    private List<List<Order>> initializePopulation(List<Order> orders, int populationSize) {
        List<List<Order>> population = new ArrayList<>(populationSize);

        // Cá thể đầu tiên = thứ tự gốc (seed heuristic)
        population.add(new ArrayList<>(orders));

        // Phần còn lại = random shuffle
        for (int i = 1; i < populationSize; i++) {
            List<Order> individual = new ArrayList<>(orders);
            Collections.shuffle(individual, random);
            population.add(individual);
        }

        return population;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ĐÁNH GIÁ FITNESS
    // ═════════════════════════════════════════════════════════════════════════
    // Decode chromosome thành các tuyến, tính tổng cost, trả về fitness.
    // fitness = 1 / totalCost → cost thấp = fitness cao
    //
    // Tại sao dùng 1/cost thay vì -cost hoặc maxCost - cost?
    //   → 1/cost đảm bảo fitness > 0 (cần cho tournament selection)
    //   → Scale tự nhiên: cost giảm 50% → fitness tăng gấp đôi
    // ═════════════════════════════════════════════════════════════════════════
    private double evaluateFitness(
            List<Order> chromosome,
            List<Vehicle> vehicles,
            Location depotLocation,
            Map<String, DistanceMatrix> distanceMap,
            LocalDate routeDate,
            LocalTime depotStartTime) {

        VrpSolutionDto solution = decodeToSolution(
                chromosome, vehicles, depotLocation, distanceMap,
                routeDate, depotStartTime);

        double totalCost = solution.getTotalCostVnd();

        // Phạt nặng cho đơn chưa phân → GA cực kỳ muốn phân hết đơn
        totalCost += solution.getUnassignedOrderCount() * UNASSIGNED_PENALTY;

        return totalCost > 0 ? 1.0 / totalCost : 0;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // DECODE CHROMOSOME → VRP SOLUTION
    // ═════════════════════════════════════════════════════════════════════════
    // Quét chromosome từ trái sang phải, gán đơn vào xe hiện tại.
    // Khi đơn không khả thi (vượt tải, vượt dung tích, trễ deadline) →
    // chuyển sang xe tiếp theo.
    //
    // Tại sao quét tuần tự thay vì dùng bin-packing phức tạp hơn?
    //   → Đơn giản, deterministic, nhanh (cần gọi hàng nghìn lần/thế hệ)
    //   → GA đã tối ưu THỨ TỰ trong chromosome → thứ tự quét ảnh hưởng
    //     phân đơn → GA gián tiếp tối ưu phân đơn qua thứ tự
    // ═════════════════════════════════════════════════════════════════════════
    private VrpSolutionDto decodeToSolution(
            List<Order> chromosome,
            List<Vehicle> vehicles,
            Location depotLocation,
            Map<String, DistanceMatrix> distanceMap,
            LocalDate routeDate,
            LocalTime depotStartTime) {

        List<PlannedRouteDto> routes = new ArrayList<>();
        int vehicleIdx = 0;
        int orderIdx = 0;

        while (orderIdx < chromosome.size() && vehicleIdx < vehicles.size()) {
            Vehicle vehicle = vehicles.get(vehicleIdx);
            double maxWeightKg = vehicle.getVehicleType().getMaxWeightKg().doubleValue();
            double maxVolumeM3 = vehicle.getVehicleType().getMaxVolumeM3() != null
                    ? vehicle.getVehicleType().getMaxVolumeM3().doubleValue()
                    : Double.MAX_VALUE;

            double currentWeight = 0;
            double currentVolume = 0;
            double routeDistance = 0;
            long routeDuration = 0;
            Location currentLoc = depotLocation;
            LocalDateTime clock = routeDate.atTime(depotStartTime);
            List<Order> stops = new ArrayList<>();

            while (orderIdx < chromosome.size()) {
                Order order = chromosome.get(orderIdx);

                double orderWeight = order.getTotalWeightKg() != null
                        ? order.getTotalWeightKg().doubleValue() : 0;
                double orderVolume = order.getTotalVolumeM3() != null
                        ? order.getTotalVolumeM3().doubleValue() : 0;

                // Kiểm tra tải trọng
                if (currentWeight + orderWeight > maxWeightKg) break;

                // Kiểm tra dung tích
                if (currentVolume + orderVolume > maxVolumeM3) break;

                // Kiểm tra khoảng cách có tính được không
                String key = currentLoc.getId() + "-" + order.getLocation().getId();
                DistanceMatrix dm = distanceMap.get(key);
                if (dm == null) {
                    orderIdx++;
                    continue; // bỏ qua đơn không có distance data
                }

                // Kiểm tra time window
                LocalDateTime arrival = clock.plusSeconds(dm.getDurationSeconds());
                if (order.getTimeWindowTo() != null) {
                    LocalDateTime deadline = routeDate.atTime(order.getTimeWindowTo());
                    if (arrival.isAfter(deadline)) {
                        // Đơn này xe không đến kịp trên tuyến hiện tại
                        // → bỏ qua, thử đơn tiếp theo (vì có thể đơn sau gần hơn)
                        orderIdx++;
                        continue;
                    }
                }

                // Đơn khả thi → gán vào tuyến
                routeDistance += dm.getDistanceMeters().doubleValue();
                routeDuration += dm.getDurationSeconds();
                clock = arrival;

                // Xử lý chờ time window
                if (order.getTimeWindowFrom() != null) {
                    LocalDateTime windowOpen = routeDate.atTime(order.getTimeWindowFrom());
                    if (clock.isBefore(windowOpen)) {
                        routeDuration += Duration.between(clock, windowOpen).getSeconds();
                        clock = windowOpen;
                    }
                }

                // Cộng thời gian phục vụ
                int serviceMin = order.getServiceTimeMinutes() != null ? order.getServiceTimeMinutes() : 15;
                routeDuration += serviceMin * 60L;
                clock = clock.plusMinutes(serviceMin);

                stops.add(order);
                currentWeight += orderWeight;
                currentVolume += orderVolume;
                currentLoc = order.getLocation();
                orderIdx++;
            }

            if (!stops.isEmpty()) {
                // Cộng quãng về depot
                String returnKey = currentLoc.getId() + "-" + depotLocation.getId();
                DistanceMatrix returnDm = distanceMap.get(returnKey);
                if (returnDm != null) {
                    routeDistance += returnDm.getDistanceMeters().doubleValue();
                    routeDuration += returnDm.getDurationSeconds();
                }

                // Tính cost
                double fixedCost = vehicle.getVehicleType().getFixedCost() != null
                        ? vehicle.getVehicleType().getFixedCost().doubleValue() : 0;
                double costPerKm = vehicle.getVehicleType().getCostPerKm().doubleValue();
                double routeCost = fixedCost + costPerKm * (routeDistance / 1000.0);

                routes.add(PlannedRouteDto.builder()
                        .vehicle(vehicle)
                        .orderStops(stops)
                        .totalDistanceMeters(routeDistance)
                        .totalDurationSeconds(routeDuration)
                        .totalWeightKg(currentWeight)
                        .totalCostVnd(routeCost)
                        .build());
            }

            vehicleIdx++;
        }

        // Đếm đơn chưa phân
        int assignedCount = routes.stream().mapToInt(r -> r.getOrderStops().size()).sum();
        int unassigned = chromosome.size() - assignedCount;

        double totalDistance = routes.stream().mapToDouble(PlannedRouteDto::getTotalDistanceMeters).sum();
        double totalCost = routes.stream().mapToDouble(PlannedRouteDto::getTotalCostVnd).sum();

        return VrpSolutionDto.builder()
                .routes(routes)
                .totalDistanceMeters(totalDistance)
                .totalCostVnd(totalCost)
                .unassignedOrderCount(unassigned)
                .build();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TOURNAMENT SELECTION
    // ═════════════════════════════════════════════════════════════════════════
    // Chọn ngẫu nhiên TOURNAMENT_SIZE cá thể, trả về cá thể có fitness cao nhất.
    //
    // Tại sao Tournament thay vì Roulette Wheel Selection?
    //   1. Tournament không cần chuẩn hóa fitness → nhanh hơn
    //   2. Áp lực chọn lọc (selection pressure) dễ điều chỉnh qua tournament size
    //   3. Không bị "super individual" chiếm dominant quá sớm
    //   4. Duy trì diversity tốt hơn → tránh premature convergence
    // ═════════════════════════════════════════════════════════════════════════
    private List<Order> tournamentSelection(
            List<List<Order>> population, double[] fitness) {

        int bestIdx = random.nextInt(population.size());
        double bestFit = fitness[bestIdx];

        for (int i = 1; i < TOURNAMENT_SIZE; i++) {
            int idx = random.nextInt(population.size());
            if (fitness[idx] > bestFit) {
                bestFit = fitness[idx];
                bestIdx = idx;
            }
        }

        return population.get(bestIdx);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ORDER CROSSOVER (OX)
    // ═════════════════════════════════════════════════════════════════════════
    // 1. Chọn ngẫu nhiên 1 đoạn [start..end] từ parent1
    // 2. Copy đoạn đó vào child tại cùng vị trí
    // 3. Điền các đơn còn lại theo thứ tự xuất hiện trong parent2
    //
    // Ví dụ:
    //   parent1 = [1, 2, 3, 4, 5, 6, 7]
    //   parent2 = [3, 7, 5, 1, 6, 4, 2]
    //   Đoạn [2..4] từ parent1: [3, 4, 5]
    //
    //   child = [_, _, 3, 4, 5, _, _]
    //   Điền từ parent2 (bỏ 3,4,5): 7, 1, 6, 2
    //   child = [7, 1, 3, 4, 5, 6, 2]
    //
    // Tại sao OX thay vì PMX hoặc CX?
    //   → OX giữ nguyên THỨ TỰ TƯƠNG ĐỐI của các đơn từ parent2
    //   → Trong VRP, thứ tự tương đối quan trọng hơn vị trí tuyệt đối
    //     (vì decode phụ thuộc thứ tự quét)
    //   → OX bảo tồn "building blocks" tốt: nếu parent1 có cụm [3,4,5]
    //     gần nhau (tuyến tốt) → child cũng giữ cụm này
    // ═════════════════════════════════════════════════════════════════════════
    private List<Order> orderCrossover(List<Order> parent1, List<Order> parent2) {
        int size = parent1.size();
        if (size <= 2) return new ArrayList<>(parent1);

        int start = random.nextInt(size);
        int end   = random.nextInt(size);
        if (start > end) {
            int tmp = start;
            start = end;
            end = tmp;
        }

        // Tạo child với slot null
        Order[] child = new Order[size];

        // Copy đoạn [start..end] từ parent1
        Set<Long> usedIds = new HashSet<>();
        for (int i = start; i <= end; i++) {
            child[i] = parent1.get(i);
            usedIds.add(parent1.get(i).getId());
        }

        // Điền phần còn lại theo thứ tự parent2
        int childIdx = (end + 1) % size;
        for (Order order : parent2) {
            if (!usedIds.contains(order.getId())) {
                child[childIdx] = order;
                childIdx = (childIdx + 1) % size;
            }
        }

        return Arrays.asList(child);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SWAP MUTATION
    // ═════════════════════════════════════════════════════════════════════════
    // Đổi chỗ 2 đơn ngẫu nhiên trong chromosome.
    //
    // Tại sao swap thay vì inversion, insertion, hoặc scramble?
    //   → Swap: thay đổi nhỏ nhất (chỉ ảnh hưởng 2 vị trí) → exploration cân bằng
    //   → Inversion: quá aggressive → có thể phá vỡ nhiều building block
    //   → Insertion: phức tạp hơn, lợi ích tương tự swap
    //   → Scramble: quá random → exploitation kém
    //
    // Với mutationRate = 0.05 (5%), trung bình mỗi 20 cá thể mới có 1
    // bị mutation → đủ diversity mà không phá hủy structure tốt.
    // ═════════════════════════════════════════════════════════════════════════
    private void swapMutation(List<Order> chromosome) {
        int size = chromosome.size();
        if (size < 2) return;

        int i = random.nextInt(size);
        int j = random.nextInt(size);
        while (j == i) {
            j = random.nextInt(size);
        }

        Collections.swap(chromosome, i, j);
    }

    /**
     * Lấy top-N indices có fitness cao nhất (cho elitism).
     */
    private List<Integer> getTopIndices(double[] fitness, int n) {
        // Tạo list (index, fitness) rồi sort giảm dần
        List<int[]> indexed = new ArrayList<>();
        for (int i = 0; i < fitness.length; i++) {
            indexed.add(new int[]{i, 0});
        }
        // Dùng double array riêng để sort
        indexed.sort((a, b) -> Double.compare(fitness[b[0]], fitness[a[0]]));

        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < Math.min(n, indexed.size()); i++) {
            result.add(indexed.get(i)[0]);
        }
        return result;
    }

    /**
     * Chạy 2-opt trên tất cả route trong solution (6.1).
     *
     * GA tối ưu phân bổ đơn↔xe (inter-route), nhưng thứ tự stop
     * trong mỗi tuyến phụ thuộc thứ tự decode (tuần tự), chưa chắc
     * tối ưu. 2-opt cải thiện intra-route → kết hợp GA + 2-opt cho
     * kết quả tốt nhất.
     */
    private VrpSolutionDto applyTwoOptToAllRoutes(
            VrpSolutionDto solution,
            Location depotLocation,
            Map<String, DistanceMatrix> distanceMap,
            LocalDate routeDate,
            LocalTime depotStartTime) {

        List<PlannedRouteDto> improvedRoutes = new ArrayList<>();
        double totalDist = 0;
        double totalCost = 0;

        for (PlannedRouteDto route : solution.getRoutes()) {
            // Chạy 2-opt
            List<Order> improvedStops = twoOptLocalSearch.improve(
                    route.getOrderStops(), depotLocation, distanceMap,
                    routeDate, depotStartTime);

            // Tính lại distance, duration
            double routeDist = 0;
            long routeDur = 0;
            Location loc = depotLocation;
            LocalDateTime clock = routeDate.atTime(depotStartTime);

            for (Order stop : improvedStops) {
                String key = loc.getId() + "-" + stop.getLocation().getId();
                DistanceMatrix dm = distanceMap.get(key);
                if (dm != null) {
                    routeDist += dm.getDistanceMeters().doubleValue();
                    routeDur += dm.getDurationSeconds();
                    clock = clock.plusSeconds(dm.getDurationSeconds());
                }

                if (stop.getTimeWindowFrom() != null) {
                    LocalDateTime windowOpen = routeDate.atTime(stop.getTimeWindowFrom());
                    if (clock.isBefore(windowOpen)) {
                        routeDur += Duration.between(clock, windowOpen).getSeconds();
                        clock = windowOpen;
                    }
                }

                int svcMin = stop.getServiceTimeMinutes() != null ? stop.getServiceTimeMinutes() : 15;
                routeDur += svcMin * 60L;
                clock = clock.plusMinutes(svcMin);

                loc = stop.getLocation();
            }

            // Quay về depot
            String returnKey = loc.getId() + "-" + depotLocation.getId();
            DistanceMatrix returnDm = distanceMap.get(returnKey);
            if (returnDm != null) {
                routeDist += returnDm.getDistanceMeters().doubleValue();
                routeDur += returnDm.getDurationSeconds();
            }

            // Tính cost
            Vehicle v = route.getVehicle();
            double fixedCost = v.getVehicleType().getFixedCost() != null
                    ? v.getVehicleType().getFixedCost().doubleValue() : 0;
            double costPerKm = v.getVehicleType().getCostPerKm().doubleValue();
            double routeCost = fixedCost + costPerKm * (routeDist / 1000.0);

            improvedRoutes.add(PlannedRouteDto.builder()
                    .vehicle(v)
                    .orderStops(improvedStops)
                    .totalDistanceMeters(routeDist)
                    .totalDurationSeconds(routeDur)
                    .totalWeightKg(route.getTotalWeightKg())
                    .totalCostVnd(routeCost)
                    .build());

            totalDist += routeDist;
            totalCost += routeCost;
        }

        return VrpSolutionDto.builder()
                .routes(improvedRoutes)
                .totalDistanceMeters(totalDist)
                .totalCostVnd(totalCost)
                .unassignedOrderCount(solution.getUnassignedOrderCount())
                .build();
    }
}
