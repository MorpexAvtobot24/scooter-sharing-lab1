package com.course.app.scooter;

import com.course.app.web.WebMvcConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.context.TestConfiguration;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScooterController.class)
@Import({WebMvcConfiguration.class, ScooterControllerTest.MethodSecurityTestConfiguration.class})
class ScooterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScooterService service;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void create_adminRequest_routesToServiceAndReturnsCreated() throws Exception {
        ScooterDto dto = new ScooterDto(17L, "SC-17", "Model X", 90, new BigDecimal("0.25"),
                ScooterStatus.AVAILABLE);
        when(service.create(org.mockito.ArgumentMatchers.any(ScooterDto.class))).thenReturn(dto);

        mockMvc.perform(post("/api/scooters")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "serialNumber": "SC-17",
                                  "model": "Model X",
                                  "batteryLevel": 90,
                                  "pricePerMinute": 0.25,
                                  "status": "AVAILABLE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/scooters/17"))
                .andExpect(jsonPath("$.id").value(17));
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void create_nonAdminRequest_isForbiddenByCompositeAnnotation() throws Exception {
        mockMvc.perform(post("/api/scooters")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "serialNumber": "SC-17",
                                  "model": "Model X",
                                  "batteryLevel": 90,
                                  "pricePerMinute": 0.25,
                                  "status": "AVAILABLE"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void findAll_addsCurrentActorResponseHeader() throws Exception {
        when(service.findAll()).thenReturn(List.of(
                new ScooterDto(1L, "SC-1", "Model X", 70, new BigDecimal("0.25"), ScooterStatus.AVAILABLE)));

        mockMvc.perform(get("/api/scooters"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Actor", "alice"))
                .andExpect(jsonPath("$[0].serialNumber").value("SC-1"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void create_availableScooterWithLowBattery_returnsDescriptiveValidationMessage() throws Exception {
        mockMvc.perform(post("/api/scooters")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "serialNumber": "SC-18",
                                  "model": "Model X",
                                  "batteryLevel": 19,
                                  "pricePerMinute": 0.25,
                                  "status": "AVAILABLE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Available scooters must have a battery level of at least 20"));
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfiguration {
    }
}
