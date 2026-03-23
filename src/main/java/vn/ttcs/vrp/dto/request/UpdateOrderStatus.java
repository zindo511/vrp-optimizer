package vn.ttcs.vrp.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.ttcs.vrp.enums.OrderStatus;

@Getter
@Setter
@NoArgsConstructor
public class UpdateOrderStatus {

    @NotNull(message = "Tình trạng đơn hàng không được để trống")
    private OrderStatus orderStatus;
}
