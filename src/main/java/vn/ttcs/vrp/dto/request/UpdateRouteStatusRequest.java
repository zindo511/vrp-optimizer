package vn.ttcs.vrp.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.ttcs.vrp.enums.RouteStatus;

@Getter
@Setter
@NoArgsConstructor
public class UpdateRouteStatusRequest {

    @NotNull(message = "Trạng thái của tuyến đường không được để trống")
    private RouteStatus status;
}
