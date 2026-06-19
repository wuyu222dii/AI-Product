package com.aipm.cowriting.common.error;

import java.util.Map;

public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final int httpStatus;
    private final Map<String, Object> details;

    public BusinessException(ErrorCode errorCode, int httpStatus, String message) {
        this(errorCode, httpStatus, message, Map.of());
    }

    public BusinessException(ErrorCode errorCode, int httpStatus, String message, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.details = details;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
