package com.cbrviewer.exception;

public class LlmConfigTestException extends RuntimeException {
    public LlmConfigTestException(String message) {
        super(message);
    }

    public LlmConfigTestException(String message, Throwable cause) {
        super(message, cause);
    }
}
