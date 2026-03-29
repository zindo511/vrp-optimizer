package vn.ttcs.vrp.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ttcs.vrp.dto.Coordinate;
import vn.ttcs.vrp.dto.request.LocationRequest;
import vn.ttcs.vrp.dto.request.UpdateLocationRequest;
import vn.ttcs.vrp.dto.response.LocationResponse;
import vn.ttcs.vrp.exception.BadRequestException;
import vn.ttcs.vrp.exception.ResourceNotFoundException;
import vn.ttcs.vrp.mapper.LocationMapper;
import vn.ttcs.vrp.model.Location;
import vn.ttcs.vrp.repository.LocationRepository;
import vn.ttcs.vrp.service.GeocodingService;
import vn.ttcs.vrp.service.LocationService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "LOCATION-SERVICE-IMPL")
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;
    private final LocationMapper locationMapper;
    private final GeocodingService geocodingService;

    @Override
    @Transactional
    public LocationResponse createLocation(LocationRequest request) {

        // Nếu Client không truyền vĩ/kinh độ, dùng Geocoding tự tính địa chỉ
        if (request.getLatitude()== null || request.getLongitude() == null) {
            log.info("Client chỉ truyền Address, tự động dò toạ độ: {}", request.getAddress());
            Coordinate coordinate = geocodingService.geocode(request.getAddress());
            if (coordinate == null) {
                throw new BadRequestException("Địa chỉ không được tìm thấy, vui lòng nhập tay");
            }

            // điền vào request
            request.setLatitude(BigDecimal.valueOf(coordinate.getLatitude()));
            request.setLongitude(BigDecimal.valueOf(coordinate.getLongitude()));
        }

        // làm tròn 4 chữ số thập phần (bán kính khoảng 11 mét)
        BigDecimal snappedLat = request.getLatitude().setScale(4, RoundingMode.HALF_UP);
        BigDecimal snappedLong = request.getLongitude().setScale(4, RoundingMode.HALF_UP);

        // chặn sinh rác: kiểm tra xem lat và lon đã có trog db chưa
        Optional<Location> duplicatedCheck = locationRepository.findByLatitudeAndLongitude(snappedLat, snappedLong);
        if (duplicatedCheck.isPresent()) {
            Location existsLoc = duplicatedCheck.get();
            log.info("Tái sử dụng cho toạ độ: {}, {}, không cần tạo cái mới nữa", snappedLat, snappedLong);
            return locationMapper.toResponse(existsLoc);
        }

        Location location = Location.builder()
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();
        return locationMapper.toResponse(locationRepository.save(location));
    }

    @Override
    @Transactional(readOnly = true)
    public LocationResponse getLocationById(Long locationId) {

        Location location = findLocationById(locationId);

        return locationMapper.toResponse(location);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LocationResponse> findAllLocations(int page, int size, String sortBy, String sortDirection) {

        Sort.Direction direction = sortDirection.equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);

        if (page < 1) page = 1;
        page = page - 1;

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Location> locationPage = locationRepository.findAll(pageable);
        return locationPage.map(locationMapper::toResponse);
    }

    @Override
    @Transactional
    public LocationResponse updateLocation(Long locationId, UpdateLocationRequest request) {

        Location location = findLocationById(locationId);
        if (request.getAddress() != null) {
            location.setAddress(request.getAddress());
        }
        if (request.getLatitude() != null) {
            location.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            location.setLongitude(request.getLongitude());
        }
        return locationMapper.toResponse(locationRepository.save(location));
    }

    @Override
    @Transactional
    public void deleteLocation(Long locationId) {

        Location location = findLocationById(locationId);
        locationRepository.delete(location);
    }


    // ==== PRIVATE HELPER METHOD ====
    private Location findLocationById(Long locationId) {
        return locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location không tìm thấy"));
    }


}
