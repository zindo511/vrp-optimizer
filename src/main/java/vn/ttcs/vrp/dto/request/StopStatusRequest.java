package vn.ttcs.vrp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.ttcs.vrp.enums.RouteStopStatus;

@Getter
@Setter
@NoArgsConstructor
public class StopStatusRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private RouteStopStatus stopStatus; // GIAO XONG/THẤT BẠI

    private String proofImageUrl;

    private String note;

    private String failureReason;
}
