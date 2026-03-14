package vn.ttcs.vrp.mapper;

import org.mapstruct.Mapper;
import vn.ttcs.vrp.dto.response.LocationResponse;
import vn.ttcs.vrp.model.Location;

@Mapper(componentModel = "spring")
public interface LocationMapper {

    LocationResponse toResponse(Location location);
}
