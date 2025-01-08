package com.baber.apigateway.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ErrorResponse {
    private boolean success;
    private String message;
    private int errorCode;
    private String errorMessage;
    private Object data;
    private String timestamp;
    private String path;
    private int status;
    private String error;
    private String requestId;
    private String errorDetails;

    public ErrorResponse(boolean success, String message, int errorCode, String errorMessage, Object data,
                            String timestamp, String path, int status, String error, String requestId,
                            String errorDetails) {
        this.success = success;
        this.message = message;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.data = data;
        this.timestamp = timestamp;
        this.path = path;
        this.status = status;
        this.error = error;
        this.requestId = requestId;
        this.errorDetails = errorDetails;
    }
}
