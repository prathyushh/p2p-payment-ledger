package com.p2ppayment.controller;

import com.p2ppayment.dto.RecipientSearchResponse;
import com.p2ppayment.entity.User;
import com.p2ppayment.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Search for recipients by name, email, or phone number
     * Used for autocomplete in Send Money page
     * @param query search term
     * @return list of matching users (max 10)
     */
    @GetMapping("/search")
    public ResponseEntity<List<RecipientSearchResponse>> searchRecipients(@RequestParam String query) {
        // Get current user to exclude them from results
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.parseLong(authentication.getName());

        if (query == null || query.trim().length() < 2) {
            return ResponseEntity.ok(List.of());
        }

        List<User> users = userRepository.searchRecipients(query.trim());

        // Map to response DTOs, exclude current user, limit to 10 results
        List<RecipientSearchResponse> responses = users.stream()
                .filter(user -> !user.getId().equals(currentUserId))
                .limit(10)
                .map(user -> new RecipientSearchResponse(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getPhoneNumber()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }
}
