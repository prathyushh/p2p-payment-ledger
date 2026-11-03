package com.p2ppayment.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleResponseStatusException_MapsStatusAndFriendlyMessage() {
        ResponseStatusException exception =
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleResponseStatusException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals(
                "You don't have enough balance to complete this transaction. Please add funds to your wallet.",
                response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void handleValidationExceptions_ReturnsFieldErrors() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError emailError = new FieldError("request", "email", "Email is required");
        FieldError phoneError = new FieldError("request", "phoneNumber", "Phone is invalid");

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(emailError, phoneError));

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleValidationExceptions(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Please check your input and try again", response.getBody().getMessage());
        assertEquals(Map.of(
                "email", "Email is required",
                "phoneNumber", "Phone is invalid"), response.getBody().getErrors());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void handleBadCredentials_ReturnsUnauthorized() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleBadCredentials(new BadCredentialsException("bad credentials"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().getStatus());
        assertEquals(
                "Invalid email or password. Please check your credentials and try again.",
                response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void handleAccessDenied_ReturnsForbidden() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleAccessDenied(new AccessDeniedException("denied"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().getStatus());
        assertEquals(
                "You don't have permission to access this resource. Please contact support if you believe this is an error.",
                response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void handleIllegalArgument_MapsFriendlyMessage() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleIllegalArgument(new IllegalArgumentException("Amount must be greater than 0"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Please enter an amount greater than zero.", response.getBody().getMessage());
    }

    @Test
    void handleGenericException_ReturnsInternalServerError() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleGenericException(new Exception("internal failure"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().getStatus());
        assertEquals(
                "We encountered an unexpected error. Please try again or contact support if the problem persists.",
                response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void friendlyMessages_CoverCommonPatterns() {
        assertFriendly("Daily send limit exceeded", "You've reached your daily sending limit of ₹50,000. Your limit will reset tomorrow.");
        assertFriendly("50000 limit", "You've reached your daily sending limit of ₹50,000. Your limit will reset tomorrow.");
        assertFriendly("Duplicate transfer detected", "This looks like a duplicate transaction. Please wait a few minutes before trying again.");
        assertFriendly("duplicate request", "This looks like a duplicate transaction. Please wait a few minutes before trying again.");
        assertFriendly("Insufficient balance", "You don't have enough balance to complete this transaction. Please add funds to your wallet.");
        assertFriendly("wallet is frozen", "This wallet has been temporarily frozen. Please contact support for assistance.");
        assertFriendly("FROZEN", "This wallet has been temporarily frozen. Please contact support for assistance.");
        assertFriendly("wallet is not active", "This wallet is currently inactive. Please activate it before making transactions.");
        assertFriendly("INACTIVE", "This wallet is currently inactive. Please activate it before making transactions.");
        assertFriendly("User not found", "We couldn't find a user with those details. Please check the email or phone number and try again.");
        assertFriendly("Recipient not found", "We couldn't find a user with those details. Please check the email or phone number and try again.");
        assertFriendly("Wallet not found", "Wallet not found. Please ensure the account is properly set up.");
        assertFriendly("Invalid PIN", "The PIN you entered is incorrect. Please try again.");
        assertFriendly("PIN is required", "The PIN you entered is incorrect. Please try again.");
        assertFriendly("Cannot send money to yourself", "You cannot send money to your own account. Please choose a different recipient.");
        assertFriendly("Bank account not found", "The bank account you selected was not found. Please add a bank account first.");
        assertFriendly("Bank account is not active", "This bank account is no longer active. Please use a different account.");
        assertFriendly("Auto reload is in progress", "An automatic reload is already in progress. Please wait a moment.");
        assertFriendly("Amount must be greater than 0", "Please enter an amount greater than zero.");
        assertFriendly("Amount must be positive", "Please enter a valid positive amount.");
        assertFriendly("Account is locked until tomorrow", "Your account has been temporarily locked due to multiple failed login attempts. Please try again later or contact support.");
        assertFriendly("locked until tomorrow", "Your account has been temporarily locked due to multiple failed login attempts. Please try again later or contact support.");
        assertFriendly("Email already exists", "This email address is already registered. Please use a different email or try logging in.");
        assertFriendly("Phone number already exists", "This phone number is already registered. Please use a different number or try logging in.");
        assertFriendly("Transaction not found", "We couldn't find that transaction. It may have been cancelled or doesn't exist.");
        assertFriendly("Transaction status must be PENDING", "This transaction cannot be settled because it's already been processed.");
    }

    @Test
    void friendlyMessage_NullReason_ReturnsGenericMessage() {
        ResponseStatusException exception =
                new ResponseStatusException(HttpStatus.BAD_REQUEST, (String) null);

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleResponseStatusException(exception);

        assertEquals("An error occurred. Please try again.", response.getBody().getMessage());
    }

    @Test
    void friendlyMessage_UnmatchedTechnicalPrefixIsCleanedAndCapitalized() {
        assertFriendly("java.lang.some useful message", "Some useful message.");
        assertFriendly("400 bad request message", "Bad request message.");
        assertFriendly("hi", "An error occurred while processing your request. Please try again.");
        assertFriendly("IllegalArgumentException happened", "An error occurred while processing your request. Please try again.");
        assertFriendly("SomeError occurred", "An error occurred while processing your request. Please try again.");
        assertFriendly("already correct.", "Already correct.");
        assertFriendly("please try again!", "Please try again!");
        assertFriendly("is this correct?", "Is this correct?");
    }

    private void assertFriendly(String technicalMessage, String expectedMessage) {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleIllegalArgument(new IllegalArgumentException(technicalMessage));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(expectedMessage, response.getBody().getMessage(),
                "Unexpected friendly message for input: " + technicalMessage);
    }
}
