package vn.ttcs.vrp.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshTokenRequest {

    @NotBlank(message = "RefreshToken không được để trống")
    private String refreshToken;
}
