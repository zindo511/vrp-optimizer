package vn.ttcs.vrp.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI vrpOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("VRP Optimizer API")
                        .description("Hệ thống tối ưu hóa tuyến đường giao hàng (Vehicle Routing Problem)")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("TTCS Team")
                                .email("huyquang2k5@gmail.com")))
                // Khai báo scheme JWT Bearer — xuất hiện nút "Authorize" trên Swagger UI
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Nhập JWT token lấy từ POST /api/auth/login")));
    }
}
