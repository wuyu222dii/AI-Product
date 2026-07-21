package com.aipm.cowriting.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aipm.cowriting.application.dto.user.CurrentUserResponse;
import com.aipm.cowriting.application.dto.user.UpdateCurrentUserRequest;
import com.aipm.cowriting.application.service.UserProfileApplicationService;
import com.aipm.cowriting.common.web.GlobalExceptionHandler;
import com.aipm.cowriting.config.SecurityConfig;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CurrentUserController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class CurrentUserControllerTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired private MockMvc mockMvc;
    @MockBean private UserProfileApplicationService service;

    @Test
    void unauthenticatedApiRequestShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void validJwtShouldReadCurrentProfile() throws Exception {
        when(service.getCurrent()).thenReturn(response("研究者"));
        mockMvc.perform(get("/api/v1/me").with(jwt().jwt(token -> token
                        .subject(USER_ID.toString())
                        .audience(List.of("authenticated")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.displayName").value("研究者"));
    }

    @Test
    void validJwtShouldUpdateDisplayName() throws Exception {
        when(service.update(any(UpdateCurrentUserRequest.class))).thenReturn(response("新的名称"));
        mockMvc.perform(patch("/api/v1/me")
                        .with(jwt().jwt(token -> token.subject(USER_ID.toString()).audience(List.of("authenticated"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"新的名称\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("新的名称"));
    }

    private CurrentUserResponse response(String displayName) {
        OffsetDateTime now = OffsetDateTime.now();
        return new CurrentUserResponse(USER_ID, "user@example.com", displayName, null, now, now);
    }
}
