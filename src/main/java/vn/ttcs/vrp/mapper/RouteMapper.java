package vn.ttcs.vrp.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.ttcs.vrp.dto.response.RouteResponse;
import vn.ttcs.vrp.model.Route;

@Mapper(componentModel = "spring", uses = { RouteStopMapper.class })
public interface RouteMapper {

    @Mapping(target = "vehicleLicensePlate", source = "vehicle.licensePlate")
    @Mapping(target = "vehicleTypeName", source = "vehicleType.name")
    @Mapping(target = "driverName", source = "driver.user.fullName")
    @Mapping(target = "driverPhone", source = "driver.phone")
    @Mapping(target = "totalDistanceKm", expression =
            "java(route.totalDistanceMeters != null ? route.totalDistanceMeters.doubleValue() / 1000 : null)")
    @Mapping(target = "totalDurationMinutes", expression = "java(route.getTotalDurationSeconds() != null ? route.getTotalDurationSeconds() / 60 : null)")
    @Mapping(target = "totalWeightKg", expression = "java(route.getTotalWeightKg() != null ? route.getTotalWeightKg().doubleValue() : null)")
    RouteResponse toRouteResponse(Route route);
}
