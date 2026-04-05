package vn.ttcs.vrp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class LocationRequest {

    @NotBlank(message = "Địa chỉ không được bỏ trống")
    private String address;

    private BigDecimal latitude;

    private BigDecimal longitude;
}
