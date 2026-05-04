package vn.ttcs.vrp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.ttcs.vrp.dto.request.LocationRequest;
import vn.ttcs.vrp.dto.response.LocationResponse;
import vn.ttcs.vrp.security.JwtAuthFilter;
import vn.ttcs.vrp.security.JwtUtils;
import vn.ttcs.vrp.security.RateLimitFilter;
import vn.ttcs.vrp.security.UserDetailsServiceImpl;
import vn.ttcs.vrp.service.LocationService;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Tests — LocationController (MockMvc)
 *
 * ═══════════════════════════════════════════════════════════════════
 * @WebMvcTest chỉ load Web layer — không cần DB.
 *
 * Test coverage:
 *   1. Input Validation (400 khi thiếu field hoặc giá trị ngoài phạm vi)
 *   2. Success path (201 khi ADMIN tạo location hợp lệ)
 *   3. Response JSON format
 * ═══════════════════════════════════════════════════════════════════
 */
@WebMvcTest(LocationController.class)
@DisplayName("Location API — Integration Tests")
class LocationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private LocationService locationService;
    @MockitoBean private JwtUtils jwtUtils;
    @MockitoBean private UserDetailsServiceImpl userDetailsService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    // ═══════════════════════════════════════════════════════════════════
    // Input Validation
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Thiếu address → 400 Bad Request")
    void missingAddress_returns400() throws Exception {
        LocationRequest request = new LocationRequest();
        request.setLatitude(BigDecimal.valueOf(10.77));
        request.setLongitude(BigDecimal.valueOf(106.70));

        mockMvc.perform(post("/api/locations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Latitude ngoài phạm vi VN (50°N) → 400")
    void invalidLatitude_returns400() throws Exception {
        LocationRequest request = new LocationRequest();
        request.setAddress("Test Location");
        request.setLatitude(BigDecimal.valueOf(50.0));
        request.setLongitude(BigDecimal.valueOf(106.70));

        mockMvc.perform(post("/api/locations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Longitude ngoài phạm vi VN (200°E) → 400")
    void invalidLongitude_returns400() throws Exception {
        LocationRequest request = new LocationRequest();
        request.setAddress("Test Location");
        request.setLatitude(BigDecimal.valueOf(10.77));
        request.setLongitude(BigDecimal.valueOf(200.0));

        mockMvc.perform(post("/api/locations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Thiếu latitude → 400")
    void missingLatitude_returns400() throws Exception {
        LocationRequest request = new LocationRequest();
        request.setAddress("123 Nguyen Hue");
        request.setLongitude(BigDecimal.valueOf(106.70));

        mockMvc.perform(post("/api/locations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Body rỗng → 400")
    void emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/api/locations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ═══════════════════════════════════════════════════════════════════
    // Success Path
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN tạo location hợp lệ → 201 Created")
    void admin_createValidLocation_returns201() throws Exception {
        LocationRequest request = validRequest();

        LocationResponse mockResponse = LocationResponse.builder()
                .id(1L)
                .address("123 Nguyen Hue, Q1, HCM")
                .latitude(10.77)
                .longitude(106.70)
                .build();

        when(locationService.createLocation(any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/locations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.address").value("123 Nguyen Hue, Q1, HCM"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Location ở biên latitude hợp lệ (8.0) → 201")
    void boundaryLatitude_valid_returns201() throws Exception {
        LocationRequest request = new LocationRequest();
        request.setAddress("Cà Mau — cực Nam VN");
        request.setLatitude(BigDecimal.valueOf(8.0));
        request.setLongitude(BigDecimal.valueOf(105.0));

        LocationResponse mockResponse = LocationResponse.builder()
                .id(2L)
                .address("Cà Mau — cực Nam VN")
                .latitude(8.0)
                .longitude(105.0)
                .build();

        when(locationService.createLocation(any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/locations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(2));
    }

    // ═══════════════════════════════════════════════════════════════════
    // Helper
    // ═══════════════════════════════════════════════════════════════════

    private LocationRequest validRequest() {
        LocationRequest request = new LocationRequest();
        request.setAddress("123 Nguyen Hue, Q1, HCM");
        request.setLatitude(BigDecimal.valueOf(10.77));
        request.setLongitude(BigDecimal.valueOf(106.70));
        return request;
    }
}
