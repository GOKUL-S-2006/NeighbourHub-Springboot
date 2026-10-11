package com.neighbourhub.exception;

import org.springframework.http.HttpStatus;

/**
 * Error with a status code and the JSON key the Node API used for the message
 * ("message" in some endpoints, "error" in others), so response bodies stay identical.
 */
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String key;

    public ApiException(HttpStatus status, String key, String message) {
        super(message);
        this.status = status;
        this.key = key;
    }

    public static ApiException message(HttpStatus status, String message) {
        return new ApiException(status, "message", message);
    }

    public static ApiException error(HttpStatus status, String message) {
        return new ApiException(status, "error", message);
    }

    public HttpStatus getStatus() { return status; }
    public String getKey() { return key; }
}
