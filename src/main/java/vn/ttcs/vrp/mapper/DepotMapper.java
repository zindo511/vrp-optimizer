package vn.ttcs.vrp.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.ttcs.vrp.dto.response.DepotResponse;
import vn.ttcs.vrp.model.Depot;

@Mapper(componentModel = "spring")
public interface DepotMapper {

    @Mapping(target = "address", source = "location.address")
    @Mapping(target = "latitude", source = "location.latitude")
    @Mapping(target = "longitude", source = "location.longitude")
    DepotResponse toResponse(Depot depot);
}
