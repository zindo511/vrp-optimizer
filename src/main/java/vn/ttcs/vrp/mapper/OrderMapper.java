package vn.ttcs.vrp.mapper;

import org.mapstruct.*;
import vn.ttcs.vrp.dto.request.OrderRequest;
import vn.ttcs.vrp.dto.request.UpdateOrderRequest;
import vn.ttcs.vrp.dto.response.OrderResponse;
import vn.ttcs.vrp.model.Order;

@Mapper(componentModel = "spring", uses = { LocationMapper.class })
public interface OrderMapper {

    @Mapping(target = "location", ignore = true)
    @Mapping(target = "user", ignore = true)
    Order toOrder(OrderRequest request);

    @Mapping(target = "createdBy", source = "user.id")
    OrderResponse toResponse(Order order);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "user", ignore = true)
    void updateOrder(UpdateOrderRequest request, @MappingTarget Order order);
}
