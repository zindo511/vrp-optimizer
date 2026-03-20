package vn.ttcs.vrp.mapper;

import org.mapstruct.*;
import vn.ttcs.vrp.dto.request.UpdateVehicleRequest;
import vn.ttcs.vrp.dto.request.VehicleRequest;
import vn.ttcs.vrp.dto.response.VehicleResponse;
import vn.ttcs.vrp.model.Vehicle;

@Mapper(componentModel = "spring",
        uses = { VehicleTypeMapper.class }
)
public interface VehicleMapper {

    @Mapping(target = "vehicleType", ignore = true)
    Vehicle toEntity(VehicleRequest request);

    VehicleResponse toResponse(Vehicle vehicle);

    @Mapping(target = "vehicleType", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateVehicle(@MappingTarget Vehicle vehicle, UpdateVehicleRequest request);
}
