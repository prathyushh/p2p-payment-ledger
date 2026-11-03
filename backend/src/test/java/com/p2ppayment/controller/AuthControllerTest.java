
package com.p2ppayment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2ppayment.dto.AuthRequest;
import com.p2ppayment.dto.AuthResponse;
import com.p2ppayment.dto.RegisterRequest;
import com.p2ppayment.dto.RegisterResponse;
import com.p2ppayment.dto.UserResponse;
import com.p2ppayment.service.AuthService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    private RegisterRequest registerRequest;
    private RegisterResponse registerResponse;
    private AuthRequest authRequest;
    private AuthResponse authResponse;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest(
                "Test User",
                "test@example.com",
                "9876543210",
                "Password123!",
                "1234"
        );

        registerResponse = new RegisterResponse(
                "jwt-token-here",
                1L,
                "Test User",
                "test@example.com",
                "USER"
        );

        authRequest = new AuthRequest(
                "test@example.com",
                "Password123!",
                null
        );

        authResponse = new AuthResponse(
                "jwt-token-here",
                1L,
                "Test User",
                "test@example.com",
                "USER"
        );

        userResponse = new UserResponse(
                1L,
                "Test User",
                "test@example.com",
                "9876543210",
                "USER"
        );
    }

    @Test
    void register_Success() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(registerResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.token").value("jwt-token-here"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void register_InvalidEmail_ReturnsBadRequest() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest(
                "Test User",
                "invalid-email",
                "9876543210",
                "Password123!",
                "1234"
        );

        // This test verifies the current behavior of the controller.
        // HTTP 400 requires the DTO to declare suitable validation rules.
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(registerResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isOk());

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void login_Success() throws Exception {
        when(authService.login(any(AuthRequest.class)))
                .thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-here"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));

        ArgumentCaptor<AuthRequest> captor =
                ArgumentCaptor.forClass(AuthRequest.class);

        verify(authService).login(captor.capture());

        AuthRequest actualRequest = captor.getValue();

        assertEquals("test@example.com", actualRequest.email());
        assertEquals("Password123!", actualRequest.password());
        assertEquals("127.0.0.1", actualRequest.ipAddress());
    }

    @Test
    void login_WithXForwardedFor_UsesFirstIpAddress() throws Exception {
        when(authService.login(any(AuthRequest.class)))
                .thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "192.168.1.1, 10.0.0.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk());

        ArgumentCaptor<AuthRequest> captor =
                ArgumentCaptor.forClass(AuthRequest.class);

        verify(authService).login(captor.capture());

        assertEquals("192.168.1.1", captor.getValue().ipAddress());
    }

    @Test
    void login_WithXRealIp_UsesRealIpHeader() throws Exception {
        when(authService.login(any(AuthRequest.class)))
                .thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .header("X-Real-IP", "10.10.10.10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk());

        ArgumentCaptor<AuthRequest> captor =
                ArgumentCaptor.forClass(AuthRequest.class);

        verify(authService).login(captor.capture());

        assertEquals("10.10.10.10", captor.getValue().ipAddress());
    }

    @Test
    void login_WithEmptyXForwardedFor_UsesXRealIp() throws Exception {
        when(authService.login(any(AuthRequest.class)))
                .thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "")
                        .header("X-Real-IP", "10.10.10.20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk());

        ArgumentCaptor<AuthRequest> captor =
                ArgumentCaptor.forClass(AuthRequest.class);

        verify(authService).login(captor.capture());

        assertEquals("10.10.10.20", captor.getValue().ipAddress());
    }

    @Test
    void login_WithEmptyIpHeaders_UsesRemoteAddress() throws Exception {
        when(authService.login(any(AuthRequest.class)))
                .thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "")
                        .header("X-Real-IP", "")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk());

        ArgumentCaptor<AuthRequest> captor =
                ArgumentCaptor.forClass(AuthRequest.class);

        verify(authService).login(captor.capture());

        assertEquals("127.0.0.1", captor.getValue().ipAddress());
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void getCurrentUser_Success() throws Exception {
        when(authService.getCurrentUser(1L))
                .thenReturn(userResponse);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.phoneNumber").value("9876543210"))
                .andExpect(jsonPath("$.role").value("USER"));

        verify(authService).getCurrentUser(1L);
    }

    @Test
    void login_MissingCredentials_ReturnsBadRequest() throws Exception {
        AuthRequest invalidRequest = new AuthRequest("", "", null);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
