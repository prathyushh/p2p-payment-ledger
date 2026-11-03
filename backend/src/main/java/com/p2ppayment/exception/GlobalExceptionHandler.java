package com.p2ppayment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler for the P2P Payment Application.
 * Converts technical exceptions into user-friendly error messages.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle ResponseStatusException - most common in the application
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        String userFriendlyMessage = makeUserFriendly(ex.getReason());
        
        ErrorResponse error = new ErrorResponse(
                ex.getStatusCode().value(),
                userFriendlyMessage,
                LocalDateTime.now()
        );
        
        return new ResponseEntity<>(error, ex.getStatusCode());
    }

    /**
     * Handle validation errors from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Please check your input and try again",
                LocalDateTime.now(),
                errors
        );
        
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle authentication errors (wrong password, etc.)
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Invalid email or password. Please check your credentials and try again.",
                LocalDateTime.now()
        );
        
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handle access denied errors (user trying to access admin endpoints, etc.)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "You don't have permission to access this resource. Please contact support if you believe this is an error.",
                LocalDateTime.now()
        );
        
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                makeUserFriendly(ex.getMessage()),
                LocalDateTime.now()
        );
        
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle all other uncaught exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        // Log the full exception for debugging
        System.err.println("Unhandled exception: " + ex.getClass().getName());
        ex.printStackTrace();
        
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "We encountered an unexpected error. Please try again or contact support if the problem persists.",
                LocalDateTime.now()
        );
        
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Convert technical error messages into user-friendly ones
     */
    private String makeUserFriendly(String technicalMessage) {
        if (technicalMessage == null) {
            return "An error occurred. Please try again.";
        }

        // Common patterns and their user-friendly replacements
        String message = technicalMessage;

        // Daily limit errors
        if (message.contains("Daily send limit") || message.contains("50,000") || message.contains("50000")) {
            return "You've reached your daily sending limit of ₹50,000. Your limit will reset tomorrow.";
        }

        // Duplicate transaction errors
        if (message.contains("Duplicate transfer detected") || message.contains("duplicate")) {
            return "This looks like a duplicate transaction. Please wait a few minutes before trying again.";
        }

        // Balance errors
        if (message.contains("Insufficient balance")) {
            return "You don't have enough balance to complete this transaction. Please add funds to your wallet.";
        }

        // Wallet status errors
        if (message.contains("wallet is frozen") || message.contains("FROZEN")) {
            return "This wallet has been temporarily frozen. Please contact support for assistance.";
        }

        if (message.contains("wallet is not active") || message.contains("INACTIVE")) {
            return "This wallet is currently inactive. Please activate it before making transactions.";
        }

        // User/recipient not found
        if (message.contains("not found") && (message.contains("User") || message.contains("Recipient"))) {
            return "We couldn't find a user with those details. Please check the email or phone number and try again.";
        }

        if (message.contains("Wallet not found")) {
            return "Wallet not found. Please ensure the account is properly set up.";
        }

        // PIN errors
        if (message.contains("Invalid PIN") || message.contains("PIN is required")) {
            return "The PIN you entered is incorrect. Please try again.";
        }

        // Self-transfer
        if (message.contains("Cannot send money to yourself")) {
            return "You cannot send money to your own account. Please choose a different recipient.";
        }

        // Bank account errors
        if (message.contains("Bank account not found")) {
            return "The bank account you selected was not found. Please add a bank account first.";
        }

        if (message.contains("Bank account is not active")) {
            return "This bank account is no longer active. Please use a different account.";
        }

        // Auto reload errors
        if (message.contains("Auto reload") && message.contains("in progress")) {
            return "An automatic reload is already in progress. Please wait a moment.";
        }

        // Amount validation
        if (message.contains("Amount must be greater than 0")) {
            return "Please enter an amount greater than zero.";
        }

        if (message.contains("Amount must be positive")) {
            return "Please enter a valid positive amount.";
        }

        // Account locked
        if (message.contains("Account is locked") || message.contains("locked until")) {
            return "Your account has been temporarily locked due to multiple failed login attempts. Please try again later or contact support.";
        }

        // Email/phone already exists
        if (message.contains("Email already exists")) {
            return "This email address is already registered. Please use a different email or try logging in.";
        }

        if (message.contains("Phone number already exists")) {
            return "This phone number is already registered. Please use a different number or try logging in.";
        }

        // Transaction not found
        if (message.contains("Transaction not found")) {
            return "We couldn't find that transaction. It may have been cancelled or doesn't exist.";
        }

        // Settlement errors
        if (message.contains("Transaction status must be PENDING")) {
            return "This transaction cannot be settled because it's already been processed.";
        }

        // If no specific pattern matched, return a cleaned-up version
        return cleanTechnicalMessage(message);
    }

    /**
     * Clean up technical jargon from error messages
     */
    private String cleanTechnicalMessage(String message) {
        // Remove technical prefixes
        message = message.replaceAll("^(java\\.lang\\.|org\\.springframework\\.|com\\.p2ppayment\\.)", "");
        
        // Remove HTTP status codes from start
        message = message.replaceAll("^\\d{3}\\s+", "");
        
        // If message is too technical or empty, return generic message
        if (message.length() < 5 || message.matches(".*Exception.*") || message.matches(".*Error.*")) {
            return "An error occurred while processing your request. Please try again.";
        }
        
        // Capitalize first letter
        if (message.length() > 0) {
            message = message.substring(0, 1).toUpperCase() + message.substring(1);
        }
        
        // Ensure it ends with a period
        if (!message.endsWith(".") && !message.endsWith("!") && !message.endsWith("?")) {
            message += ".";
        }
        
        return message;
    }

    /**
     * Error response structure
     */
    public static class ErrorResponse {
        private int status;
        private String message;
        private LocalDateTime timestamp;
        private Map<String, String> errors;

        public ErrorResponse(int status, String message, LocalDateTime timestamp) {
            this.status = status;
            this.message = message;
            this.timestamp = timestamp;
        }

        public ErrorResponse(int status, String message, LocalDateTime timestamp, Map<String, String> errors) {
            this.status = status;
            this.message = message;
            this.timestamp = timestamp;
            this.errors = errors;
        }

        // Getters
        public int getStatus() { return status; }
        public String getMessage() { return message; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public Map<String, String> getErrors() { return errors; }
    }
}
