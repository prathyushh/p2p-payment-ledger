package com.p2ppayment.controller;

import com.p2ppayment.dto.RecipientSearchResponse;
import com.p2ppayment.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    private RecipientSearchResponse recipientSearchResponse;

    @BeforeEach
    void setUp() {
        recipientSearchResponse = new RecipientSearchResponse(
            2L,
            "recipient@example.com",
            "Recipient User"
        );
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void searchRecipients_Success() throws Exception {
        // Arrange
        List<RecipientSearchResponse> recipients = Arrays.asList(recipientSearchResponse);
        when(userService.searchRecipients(anyString())).thenReturn(recipients);

        // Act & Assert
        mockMvc.perform(get("/api/users/search")
                .param("query", "recipient")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(2))
                .andExpect(jsonPath("$[0].email").value("recipient@example.com"))
                .andExpect(jsonPath("$[0].fullName").value("Recipient User"));
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void searchRecipients_EmptyQuery_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/users/search")
                .param("query", "")
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void searchRecipients_TooShortQuery_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/users/search")
                .param("query", "ab")
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void searchRecipients_NoResults_ReturnsEmptyList() throws Exception {
        // Arrange
        when(userService.searchRecipients(anyString())).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/users/search")
                .param("query", "nonexistent")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
