/**
 * Extract user-friendly error message from API error response
 * @param {Error} error - The error object from axios
 * @returns {string} User-friendly error message
 */
export function getErrorMessage(error) {
  // Check if error has userMessage attached by interceptor
  if (error.userMessage) {
    return error.userMessage;
  }

  // Try to extract from response data
  if (error.response?.data) {
    const data = error.response.data;
    
    // Check for message field
    if (data.message) {
      return data.message;
    }
    
    // Check for error field
    if (data.error) {
      return data.error;
    }
    
    // Check for errors object (validation errors)
    if (data.errors && typeof data.errors === 'object') {
      const errorMessages = Object.values(data.errors).join('. ');
      if (errorMessages) {
        return errorMessages;
      }
    }
  }

  // Check for network errors
  if (error.message === 'Network Error') {
    return 'Unable to connect to the server. Please check your internet connection and try again.';
  }

  // Check for timeout errors
  if (error.code === 'ECONNABORTED') {
    return 'The request took too long to complete. Please try again.';
  }

  // Status code based messages
  if (error.response?.status) {
    const status = error.response.status;
    
    switch (status) {
      case 400:
        return 'Invalid request. Please check your input and try again.';
      case 401:
        return 'Authentication failed. Please check your credentials.';
      case 403:
        return 'You don\'t have permission to perform this action.';
      case 404:
        return 'The requested resource was not found.';
      case 409:
        return 'This action conflicts with existing data.';
      case 422:
        return 'Unable to process your request. Please check your input.';
      case 423:
        return 'This resource is currently locked.';
      case 500:
        return 'A server error occurred. Please try again later.';
      case 503:
        return 'The service is temporarily unavailable. Please try again later.';
      default:
        if (status >= 500) {
          return 'A server error occurred. Please try again later.';
        }
        if (status >= 400) {
          return 'An error occurred while processing your request.';
        }
    }
  }

  // Fallback message
  return 'An unexpected error occurred. Please try again.';
}

/**
 * Show error message in a toast or alert
 * @param {Error} error - The error object
 * @param {Function} showToast - Optional toast function
 */
export function handleError(error, showToast = null) {
  const message = getErrorMessage(error);
  
  if (showToast && typeof showToast === 'function') {
    showToast(message, 'error');
  } else {
    // Fallback to alert if no toast function provided
    alert(message);
  }
  
  // Log the full error for debugging
  console.error('API Error:', error);
  
  return message;
}
