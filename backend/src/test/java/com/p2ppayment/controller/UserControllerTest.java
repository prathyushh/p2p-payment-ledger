
package com.p2ppayment.controller;

import com.p2ppayment.entity.User;
import com.p2ppayment.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    private User recipient;

    @BeforeEach
    void setUp() {
        recipient = new User();
        recipient.setId(2L);
        recipient.setName("Recipient User");
        recipient.setEmail("recipient@example.com");
        recipient.setPhoneNumber("9876543210");
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void searchRecipients_Success() throws Exception {
        when(userRepository.searchRecipients("recipient"))
                .thenReturn(List.of(recipient));

        mockMvc.perform(get("/api/users/search")
                        .param("query", "recipient"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].name").value("Recipient User"))
                .andExpect(jsonPath("$[0].email")
                        .value("recipient@example.com"))
                .andExpect(jsonPath("$[0].phoneNumber")
                        .value("9876543210"));

        verify(userRepository).searchRecipients("recipient");
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void searchRecipients_EmptyQuery_ReturnsEmptyList()
            throws Exception {

        mockMvc.perform(get("/api/users/search")
                        .param("query", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verifyNoInteractions(userRepository);
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void searchRecipients_TooShortQuery_ReturnsEmptyList()
            throws Exception {

        mockMvc.perform(get("/api/users/search")
                        .param("query", "a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verifyNoInteractions(userRepository);
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void searchRecipients_NoResults_ReturnsEmptyList()
            throws Exception {

        when(userRepository.searchRecipients("nonexistent"))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/users/search")
                        .param("query", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(userRepository).searchRecipients("nonexistent");
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void searchRecipients_ExcludesCurrentUser()
            throws Exception {

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setName("Current User");
        currentUser.setEmail("current@example.com");
        currentUser.setPhoneNumber("9876543211");

        when(userRepository.searchRecipients("user"))
                .thenReturn(List.of(currentUser, recipient));

        mockMvc.perform(get("/api/users/search")
                        .param("query", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].email")
                        .value("recipient@example.com"));

        verify(userRepository).searchRecipients("user");
    }

    @Test
    @WithMockUser(username = "1", roles = {"USER"})
    void searchRecipients_OnlyCurrentUser_ReturnsEmptyList()
            throws Exception {

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setName("Current User");
        currentUser.setEmail("current@example.com");
        currentUser.setPhoneNumber("9876543211");

        when(userRepository.searchRecipients("current"))
                .thenReturn(List.of(currentUser));

        mockMvc.perform(get("/api/users/search")
                        .param("query", "current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(userRepository).searchRecipients("current");
    }
}
