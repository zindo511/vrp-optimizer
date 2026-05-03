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
import vn.ttcs.vrp.exception.BadRequestException;
import vn.ttcs.vrp.exception.DuplicateResourceException;
import vn.ttcs.vrp.exception.ResourceNotFoundException;
import vn.ttcs.vrp.mapper.DriverMapper;
import vn.ttcs.vrp.model.Driver;
import vn.ttcs.vrp.model.User;
import vn.ttcs.vrp.model.Vehicle;
import vn.ttcs.vrp.repository.DriverRepository;
import vn.ttcs.vrp.repository.UserRepository;
import vn.ttcs.vrp.repository.VehicleRepository;
import vn.ttcs.vrp.service.DriverService;

@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
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

        // Gán xe nếu có vehicleId
        assignVehicle(driver, request.getVehicleId());

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
            if (driverRepository.existsByLicenseNumber(request.getLicenseNumber())) {
                throw new DuplicateResourceException("Đã tồn tại giấy phép lái xe: " + request.getLicenseNumber());
            }
        }

        driverMapper.updateDriver(request, driver);

        // Gán xe nếu vehicleId được gửi lên
        if (request.getVehicleId() != null) {
            assignVehicle(driver, request.getVehicleId());
        }

        return driverMapper.toResponse(driverRepository.save(driver));
    }

    @Override
    public DriverResponse updateDriverStatus(Long id, DriverStatus driverStatus) {
        Driver driver = getDriverById(id);
        driver.setStatus(driverStatus);
        return driverMapper.toResponse(driverRepository.save(driver));
    }

    @Override
    @Transactional
    public void deleteDriver(Long id) {
        Driver driver = getDriverById(id);
        driverRepository.delete(driver);
    }

    // ==== PRIVATE HELPER ====
    private Driver getDriverById(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài xế với id: " + id));
    }

    /**
     * Gán xe cho tài xế.
     * - vehicleId == null → bỏ qua (không thay đổi)
     * - vehicleId == 0 → gỡ xe khỏi tài xế
     * - vehicleId > 0 → gán xe, kiểm tra xe chưa thuộc tài xế khác
     */
    private void assignVehicle(Driver driver, Long vehicleId) {
        if (vehicleId == null) return;

        // vehicleId = 0 → gỡ gán xe
        if (vehicleId == 0) {
            driver.setVehicle(null);
            return;
        }

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy xe với id: " + vehicleId));

        // Kiểm tra xe đã được gán cho tài xế khác chưa
        driverRepository.findByVehicle(vehicle).ifPresent(existingDriver -> {
            if (!existingDriver.getId().equals(driver.getId())) {
                throw new BadRequestException(
                        "Xe " + vehicle.getLicensePlate() + " đã được gán cho tài xế id=" + existingDriver.getId());
            }
        });

        driver.setVehicle(vehicle);
    }
}
