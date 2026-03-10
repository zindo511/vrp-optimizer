package vn.ttcs.vrp.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String jwtSecret;
    private int jwtExpirationMs;
    private long jwtRefreshExpirationMs;
}
