package vn.ttcs.vrp.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ttcs.vrp.dto.request.UpdateVehicleRequest;
import vn.ttcs.vrp.dto.request.VehicleRequest;
import vn.ttcs.vrp.dto.response.VehicleResponse;
import vn.ttcs.vrp.enums.VehicleStatus;
import vn.ttcs.vrp.exception.DuplicateResourceException;
import vn.ttcs.vrp.exception.ResourceNotFoundException;
import vn.ttcs.vrp.mapper.VehicleMapper;
import vn.ttcs.vrp.model.Vehicle;
import vn.ttcs.vrp.model.VehicleType;
import vn.ttcs.vrp.repository.VehicleRepository;
import vn.ttcs.vrp.repository.VehicleTypeRepository;
import vn.ttcs.vrp.service.VehicleService;

@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final VehicleMapper vehicleMapper;

    @Override
    @Transactional
    public VehicleResponse createVehicle(VehicleRequest request) {

        if (vehicleRepository.existsByLicensePlate(request.getLicensePlate())) {
            throw new DuplicateResourceException("Xe đã tồn tại");
        }

        VehicleType vehicleType = getVehicleType(request.getVehicleTypeId());

        Vehicle vehicle = vehicleMapper.toEntity(request);
        vehicle.setVehicleType(vehicleType);
        if (vehicle.getStatus() == null)
            vehicle.setStatus(VehicleStatus.AVAILABLE);
        return vehicleMapper.toResponse(vehicleRepository.save(vehicle));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehicleResponse> listVehiclesByStatus(int page, int size, String sortBy,
                                     String sortDirection, VehicleStatus status) {

        // tạo đối tượng sort
        Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);

        //validate page
        if (page < 1) page = 1;
        if (size < 1) size = 20;
        page = page - 1;
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Vehicle> vehiclePage;

        if (status == null) {
            vehiclePage = vehicleRepository.findAll(pageable);
        } else {
            vehiclePage = vehicleRepository.findVehicleByStatus(status, pageable);
        }
        return vehiclePage.map(vehicleMapper::toResponse);
    }

    @Override
    @Transactional
    public VehicleResponse updateVehicle(Long id, UpdateVehicleRequest request) {

        Vehicle vehicle = getVehicle(id);

        if (request.getVehicleTypeId() != null && !request.getVehicleTypeId().equals(vehicle.getVehicleType().getId())) {
            VehicleType vehicleType = getVehicleType(request.getVehicleTypeId());
            vehicle.setVehicleType(vehicleType);
        }

        if (request.getLicensePlate() != null && !request.getLicensePlate().equals(vehicle.getLicensePlate())) {
            if (vehicleRepository.existsByLicensePlate(vehicle.getLicensePlate())) {
                throw new DuplicateResourceException("Biển số xe đã tồn tại trong hệ thống");
            }
        }

        vehicleMapper.updateVehicle(vehicle, request);
        return vehicleMapper.toResponse(vehicleRepository.save(vehicle));
    }

    @Override
    @Transactional
    public VehicleResponse updateVehicleStatus(Long id, VehicleStatus status) {
        Vehicle vehicle = getVehicle(id);

        vehicle.setStatus(status);
        return vehicleMapper.toResponse(vehicleRepository.save(vehicle));
    }


    // ==== PRIVATE HEPLER ====
    private VehicleType getVehicleType(Long vehicleTypeId) {
        return vehicleTypeRepository.findById(vehicleTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Loại xe không được tìm thấy"));
    }

    private Vehicle getVehicle(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy xe với id: " + id));
    }
}
