package vn.ttcs.vrp.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import vn.ttcs.vrp.dto.request.UpdateVehicleTypeRequest;
import vn.ttcs.vrp.dto.request.VehicleTypeRequest;
import vn.ttcs.vrp.dto.response.VehicleTypeResponse;
import vn.ttcs.vrp.model.VehicleType;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface VehicleTypeMapper {

    VehicleTypeResponse toResponse(VehicleType vehicleType);

    @Mapping(target = "id", ignore = true)
    VehicleType toEntity(VehicleTypeRequest vehicleTypeRequest);

    @Mapping(target = "id", ignore = true)
    void updateVehicleTypeFromRequest(UpdateVehicleTypeRequest request, @MappingTarget VehicleType vehicleType);
}
