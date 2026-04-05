package vn.ttcs.vrp.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ttcs.vrp.dto.request.DepotRequest;
import vn.ttcs.vrp.dto.request.UpdateDepotRequest;
import vn.ttcs.vrp.dto.response.DepotResponse;
import vn.ttcs.vrp.exception.BadRequestException;
import vn.ttcs.vrp.exception.ResourceNotFoundException;
import vn.ttcs.vrp.mapper.DepotMapper;
import vn.ttcs.vrp.model.Depot;
import vn.ttcs.vrp.model.Location;
import vn.ttcs.vrp.repository.DepotRepository;
import vn.ttcs.vrp.repository.LocationRepository;
import vn.ttcs.vrp.service.DepotService;

@Service
@RequiredArgsConstructor
public class DepotServiceImpl implements DepotService {

    private final DepotRepository depotRepository;
    private final LocationRepository locationRepository;
    private final DepotMapper depotMapper;

    @Override
    @Transactional
    public DepotResponse createDepot(DepotRequest request) {

        if (request.getStartTime() != null && request.getEndTime() != null) {
            if (request.getStartTime().isAfter(request.getEndTime())) {
                throw new BadRequestException("Start time không thể sau end time");
            }
        }

        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new ResourceNotFoundException("location không được tìm thấy"));
        Depot depot = Depot.builder()
                .location(location)
                .name(request.getName())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();
        return depotMapper.toResponse(depotRepository.save(depot));
    }

    @Override
    public DepotResponse getDepotById(Long id) {
        Depot depot = depotById(id);
        return depotMapper.toResponse(depot);
    }

    @Override
    @Transactional
    public Page<DepotResponse> getAllDepots(int page, int size, String sortBy, String sortDir) {

        Sort.Direction direction = sortDir.equals("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);

        if (page <= 1) page = 1;
        page = page - 1;

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Depot> depotPage = depotRepository.findAll(pageable);
        return depotPage.map(depotMapper::toResponse);
    }

    @Override
    @Transactional
    public DepotResponse updateDepotById(Long id, UpdateDepotRequest request) {
        Depot depot = depotById(id);
        if (request.getLocationId() != null) {
            Location location = locationRepository.findById(request.getLocationId())
                            .orElseThrow(() -> new ResourceNotFoundException("Location không đuược tìm thấy"));
            depot.setLocation(location);
        }

        if (request.getStartTime() != null) {
            depot.setStartTime(request.getStartTime());
        }

        if (request.getEndTime() != null) {
            depot.setEndTime(request.getEndTime());
        }

        if (depot.getStartTime() != null && depot.getEndTime() != null) {
            if (depot.getStartTime().isAfter(depot.getEndTime())) {
                throw new BadRequestException("Thời gian bắt đầu không được sau thời gian kết thúc");
            }
        }
        return depotMapper.toResponse(depotRepository.save(depot));
    }

    @Override
    public void deleteDepotById(Long id) {
        Depot depot = depotById(id);
        depotRepository.delete(depot);
    }

    // ==== PRIVATE HELPER ====
    private Depot depotById(Long id) {
        return depotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("depot không được tìm thấy"));
    }
}
