package vn.ttcs.vrp.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.ttcs.vrp.dto.response.RouteStopResponse;
import vn.ttcs.vrp.model.RouteStop;

@Mapper(componentModel = "spring")
public interface RouteStopMapper {

    @Mapping(target = "customerName", source = "order.customerName")
    @Mapping(target = "customerPhone", source = "order.customerPhone")
    @Mapping(target = "address", source = "location.address")
    @Mapping(target = "lat", source = "location.latitude")
    @Mapping(target = "lng", source = "location.longitude")
    @Mapping(target = "weightKg", source = "order.totalWeightKg")
    RouteStopResponse toResponse(RouteStop routeStop);
}
