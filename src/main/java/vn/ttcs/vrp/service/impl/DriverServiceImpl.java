package vn.ttcs.vrp.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ttcs.vrp.dto.request.DriverRequest;
import vn.ttcs.vrp.dto.request.UpdateDriverRequest;
import vn.ttcs.vrp.dto.response.DriverResponse;
import vn.ttcs.vrp.enums.DriverStatus;
import vn.ttcs.vrp.exception.DuplicateResourceException;
import vn.ttcs.vrp.exception.ResourceNotFoundException;
import vn.ttcs.vrp.mapper.DriverMapper;
import vn.ttcs.vrp.model.Driver;
import vn.ttcs.vrp.model.User;
import vn.ttcs.vrp.repository.DriverRepository;
import vn.ttcs.vrp.repository.UserRepository;
import vn.ttcs.vrp.service.DriverService;

@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final DriverMapper driverMapper;

    @Override
    @Transactional
    public DriverResponse createDriver(DriverRequest request) {

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user với id: " + request.getUserId()));

        // Kiểm tra xem user đã gán với profile driver chưa
        if (driverRepository.existsByUser(user)) {
            throw new DuplicateResourceException("Đã tồn tại hồ sơ người dùng " + request.getUserId());
        }

        // Bằng lái phải là duy nhất
        if (driverRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new DuplicateResourceException("Đã tồn tại bằng lái xe " + request.getLicenseNumber());
        }

        Driver driver = driverMapper.toEntity(request);
        driver.setUser(user);
        if (driver.getStatus() == null)
            driver.setStatus(DriverStatus.ACTIVE);
        return driverMapper.toResponse(driverRepository.save(driver));
    }

    @Override
    @Transactional
    public Page<DriverResponse> findAllDrivers(int page, int size, String sortBy, String sortDir, DriverStatus status) {

        // tạo sort
        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);

        // tạo pageable
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, size, sort);

        Page<Driver> drivers;
        if (status == null) {
            drivers = driverRepository.findAll(pageable);
        } else {
            drivers = driverRepository.findByStatus(status, pageable);
        }
        return drivers.map(driverMapper::toResponse);
    }

    @Override
    @Transactional
    public DriverResponse findDriverById(Long id) {
        Driver driver = getDriverById(id);
        return driverMapper.toResponse(driver);
    }

    @Override
    @Transactional
    public DriverResponse updateDriverById(Long id, UpdateDriverRequest request) {

        Driver driver = getDriverById(id);

        if (request.getLicenseNumber() != null && !request.getLicenseNumber().equals(driver.getLicenseNumber())) {
            if (!driverRepository.existsByLicenseNumber(request.getLicenseNumber())) {
                throw new DuplicateResourceException("Đã tồn tại giấy phép lái xe: " + request.getLicenseNumber());
            }
        }

        driverMapper.updateDriver(request, driver);
        return driverMapper.toResponse(driverRepository.save(driver));
    }

    @Override
    public DriverResponse updateDriverStatus(Long id, DriverStatus driverStatus) {
        Driver driver = getDriverById(id);
        driver.setStatus(driverStatus);
        return driverMapper.toResponse(driverRepository.save(driver));
    }

    // ==== PRIVATE HELPER ====
    private Driver getDriverById(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài xế với id: " + id));
    }


}
