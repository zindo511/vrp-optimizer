package vn.ttcs.vrp.mapper;

import org.mapstruct.*;
import vn.ttcs.vrp.dto.request.DriverRequest;
import vn.ttcs.vrp.dto.request.UpdateDriverRequest;
import vn.ttcs.vrp.dto.response.DriverResponse;
import vn.ttcs.vrp.model.Driver;

@Mapper(componentModel = "spring")
public interface DriverMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userEmail", source = "user.email")
    @Mapping(target = "userFullName", source = "user.fullName")
    @Mapping(target = "vehicleId", source = "vehicle.id")
    @Mapping(target = "vehicleLicensePlate", source = "vehicle.licensePlate")
    DriverResponse toResponse(Driver driver);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "currentLat", ignore = true)
    @Mapping(target = "currentLng", ignore = true)
    Driver toEntity(DriverRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "currentLat", ignore = true)
    @Mapping(target = "currentLng", ignore = true)
    void updateDriver(UpdateDriverRequest request, @MappingTarget Driver driver);
}
