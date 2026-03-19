package vn.ttcs.vrp.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ttcs.vrp.dto.request.UpdateVehicleTypeRequest;
import vn.ttcs.vrp.dto.request.VehicleTypeRequest;
import vn.ttcs.vrp.dto.response.VehicleTypeResponse;
import vn.ttcs.vrp.exception.ResourceNotFoundException;
import vn.ttcs.vrp.mapper.VehicleTypeMapper;
import vn.ttcs.vrp.model.VehicleType;
import vn.ttcs.vrp.repository.VehicleTypeRepository;
import vn.ttcs.vrp.service.VehicleTypeService;

@Service
@RequiredArgsConstructor
public class VehicleTypeServiceImpl implements VehicleTypeService {

    private final VehicleTypeRepository vehicleTypeRepository;
    private final VehicleTypeMapper vehicleTypeMapper;

    @Override
    @Transactional
    public VehicleTypeResponse createVehicleType(VehicleTypeRequest request) {

        VehicleType vehicleType = vehicleTypeMapper.toEntity(request);
        if (vehicleType.getIsActive() == null) {
            vehicleType.setIsActive(true);
        }
        return vehicleTypeMapper.toResponse(vehicleTypeRepository.save(vehicleType));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehicleTypeResponse> findAllVehicleTypes(int page, int size, String sortBy, String sortDir) {

        // tạo đối tượng direction
        Sort.Direction direction = sortDir.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        //validate
        if (page < 1) page = 1;
        page = page - 1;

        // tạo pageable và truyền vào
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<VehicleType> page1 = vehicleTypeRepository.findAll(pageable);
        return page1.map(vehicleTypeMapper::toResponse);
    }

    @Override
    @Transactional
    public VehicleTypeResponse updateVehicleType(Long id, UpdateVehicleTypeRequest request) {

        VehicleType vehicleType = vehicleTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vehicle loại xe " + id));
        vehicleTypeMapper.updateVehicleTypeFromRequest(request, vehicleType);
        return vehicleTypeMapper.toResponse(vehicleTypeRepository.save(vehicleType));
    }


}
