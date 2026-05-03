package vn.ttcs.vrp.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ttcs.vrp.dto.request.AlgorithmConfigRequest;
import vn.ttcs.vrp.dto.request.UpdateAlgorithmConfigRequest;
import vn.ttcs.vrp.dto.response.AlgorithmConfigResponse;
import vn.ttcs.vrp.exception.ResourceNotFoundException;
import vn.ttcs.vrp.model.AlgorithmConfig;
import vn.ttcs.vrp.repository.AlgorithmConfigRepository;
import vn.ttcs.vrp.service.AlgorithmConfigService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "ALGORITHM-CONFIG-SERVICE")
@Transactional
public class AlgorithmConfigServiceImpl implements AlgorithmConfigService {

    private final AlgorithmConfigRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<AlgorithmConfigResponse> findAll() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AlgorithmConfigResponse create(AlgorithmConfigRequest request) {
        AlgorithmConfig config = AlgorithmConfig.builder()
                .name(request.getName())
                .populationSize(request.getPopulationSize() != null ? request.getPopulationSize() : 100)
                .generations(request.getGenerations() != null ? request.getGenerations() : 500)
                .mutationRate(request.getMutationRate() != null ? request.getMutationRate() : new java.math.BigDecimal("0.05"))
                .crossoverRate(request.getCrossoverRate() != null ? request.getCrossoverRate() : new java.math.BigDecimal("0.80"))
                .elitismCount(request.getElitismCount() != null ? request.getElitismCount() : 2)
                .isActive(request.getIsActive() != null ? request.getIsActive() : false)
                .description(request.getDescription())
                .build();

        config = repository.save(config);
        log.info("Tạo AlgorithmConfig mới: id={}, name={}", config.getId(), config.getName());
        return toResponse(config);
    }

    @Override
    public AlgorithmConfigResponse update(Long id, UpdateAlgorithmConfigRequest request) {
        AlgorithmConfig config = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy AlgorithmConfig id: " + id));

        // PATCH: chỉ cập nhật field nào được gửi lên (non-null)
        if (request.getName() != null)           config.setName(request.getName());
        if (request.getPopulationSize() != null)  config.setPopulationSize(request.getPopulationSize());
        if (request.getGenerations() != null)     config.setGenerations(request.getGenerations());
        if (request.getMutationRate() != null)     config.setMutationRate(request.getMutationRate());
        if (request.getCrossoverRate() != null)    config.setCrossoverRate(request.getCrossoverRate());
        if (request.getElitismCount() != null)     config.setElitismCount(request.getElitismCount());
        if (request.getIsActive() != null)         config.setIsActive(request.getIsActive());
        if (request.getDescription() != null)      config.setDescription(request.getDescription());

        config = repository.save(config);
        log.info("Cập nhật AlgorithmConfig: id={}, name={}", config.getId(), config.getName());
        return toResponse(config);
    }

    private AlgorithmConfigResponse toResponse(AlgorithmConfig config) {
        return AlgorithmConfigResponse.builder()
                .id(config.getId())
                .name(config.getName())
                .populationSize(config.getPopulationSize())
                .generations(config.getGenerations())
                .mutationRate(config.getMutationRate())
                .crossoverRate(config.getCrossoverRate())
                .elitismCount(config.getElitismCount())
                .isActive(config.getIsActive())
                .description(config.getDescription())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
