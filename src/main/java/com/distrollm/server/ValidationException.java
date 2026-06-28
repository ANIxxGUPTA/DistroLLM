package com.distrollm.server;

public class ValidationException extends RuntimeException {
    private final String errorMessage;
    private final int statusCode;

    public ValidationException(String errorMessage, int statusCode) {
        super(errorMessage);
        this.errorMessage = errorMessage;
        this.statusCode = statusCode;
    }

    public String getErrorMessage() { return errorMessage; }
    public int getStatusCode() { return statusCode; }
}
